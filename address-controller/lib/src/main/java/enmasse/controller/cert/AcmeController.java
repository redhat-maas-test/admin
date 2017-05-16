package enmasse.controller.cert;

import enmasse.config.LabelKeys;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Instance;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages Let's Encrypt certificates for an EnMasse instance
 */
public class AcmeController extends InstanceWatcher implements Handler<HttpServerRequest> {
    private static final Logger log = LoggerFactory.getLogger(AcmeController.class.getName());
    private final KeyPair keyPair;
    private final Session session;
    private final Registration registration;
    private final OpenShiftClient client;
    private final Map<URI, Http01Challenge> challengeMap = new LinkedHashMap<>();
    private HttpServer server;
    private final Kubernetes kubernetes;

    public AcmeController(KeyPair keyPair, Session session, Registration registration, OpenShiftClient client, Kubernetes kubernetes) {
        super(client);
        this.keyPair = keyPair;
        this.session = session;
        this.registration = registration;
        this.client = client;
        this.kubernetes = kubernetes;
    }

    @Override
    public void start() {
        super.start();
        server = vertx.createHttpServer()
                .requestHandler(this)
                .listen(12345);
    }

    @Override
    public void stop() {
        super.stop();
        if (server != null) {
            server.close();
        }
    }


    protected void instanceChanged(Instance instance) throws AcmeException, InterruptedException, IOException {
        createChallenge(instance, instance.messagingHost());
        //createChallenge(instance.mqttHost());
    }

    protected void instanceDeleted(Instance instance) throws AcmeException, InterruptedException {
        deleteChallenge(instance, instance.messagingHost());
       // deleteChallenge(instance.mqttHost());
    }

    private void createChallenge(Instance instance, Optional<String> domain) throws AcmeException, InterruptedException, IOException {
        if (domain.isPresent() && !"".equals(domain.get())) {
            createChallenge(instance, domain.get());
        }
    }

    private void deleteChallenge(Instance instance, Optional<String> domain) throws AcmeException, InterruptedException {
        if (domain.isPresent() && !"".equals(domain.get())) {
            deleteChallenge(instance, domain.get());
        }
    }

    private void createChallenge(Instance instance, String domain) throws AcmeException, InterruptedException, IOException {
        Authorization auth = registration.authorizeDomain(domain);

        Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
        String name = Kubernetes.sanitizeName("cert-challenge-" + domain);
        String instanceId = Kubernetes.sanitizeName(instance.id().getId());
        if (client.isAdaptable(OpenShiftClient.class)) {
            client.routes().createNew()
                    .withNewMetadata()
                    .withName(name)
                    .addToLabels(LabelKeys.TYPE, "cert-config")
                    .addToLabels(LabelKeys.DOMAIN, domain)
                    .addToLabels(LabelKeys.INSTANCE, instanceId)
                    .endMetadata()
                    .withNewSpec()
                    .withHost(challenge.getLocation().getHost())
                    .withPath(challenge.getLocation().getPath())
                    .endSpec();
        } else {
            client.extensions().ingresses().createNew()
                    .withNewMetadata()
                    .withName(name)
                    .addToLabels(LabelKeys.TYPE, "cert-config")
                    .addToLabels(LabelKeys.DOMAIN, domain)
                    .addToLabels(LabelKeys.INSTANCE, instanceId)
                    .endMetadata()
                    .withNewSpec()
                    .addNewRule()
                    .withHost(challenge.getLocation().getHost())
                    .withNewHttp()
                    .addNewPath()
                    .withPath(challenge.getLocation().getPath())
                    .endPath()
                    .endHttp()
                    .endRule()
                    .endSpec()
                    .done();
        }

        exposeChallenge(challenge.getLocation(), challenge);

        challenge.trigger();

        while (challenge.getStatus() != Status.VALID) {
            Thread.sleep(3000L);
            log.info("Challenge status " + challenge.getStatus() + ", updating");
            challenge.update();
        }

        log.info("Challenge accepted! Requesting certificates");

        KeyPair certKeyPair = KeyPairUtils.createKeyPair(2048);

        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomain(domain);
        csrb.setOrganization("EnMasse");
        csrb.sign(certKeyPair);
        byte[] csr = csrb.getEncoded();

        Certificate cert = registration.requestCertificate(csr);
        ByteArrayOutputStream certWriter = new ByteArrayOutputStream();
        CertificateUtils.writeX509Certificate(cert.download(), certWriter);

        Map<String, String> data = new LinkedHashMap<>();
        Base64.Encoder encoder = Base64.getEncoder();
        data.put("server-key.pem", encoder.encodeToString(certKeyPair.getPrivate().getEncoded()));
        data.put("server-cert.pem", encoder.encodeToString(certWriter.toByteArray()));
        String secret = instance.certSecret().get();
        client.secrets().withName(secret).edit()
                .addToData(data)
                .done();

    }

    private void deleteChallenge(Instance instance, String domain) throws AcmeException {
        Authorization.bind(session, URI.create(domain)).deactivate();
        // TODO: Revoke certificate
    }


    private void exposeChallenge(URI domain, Http01Challenge challenge) {
        challengeMap.put(domain, challenge);
    }

    public static AcmeController create(OpenShiftClient client, Kubernetes kubernetes) throws AcmeException {
        KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
        Session session = new Session("cert://letsencrypt.org/staging", keyPair);
        /**
         * TODO:
         * - Store keypair in a secret
         * - Support updating registration info
         * - Support revoking/changing keys
         * - Support deactivating account
         */


        RegistrationBuilder builder = new RegistrationBuilder();
        builder.addContact("mailto:cert@enmasse.io");

        Registration registration;
        try {
            registration = builder.create(session);
        } catch (AcmeConflictException e) {
            registration = Registration.bind(session, e.getLocation());
        }

        return new AcmeController(keyPair, session, registration, client, kubernetes);
    }

    @Override
    public void handle(HttpServerRequest request) {
        if (request.method().equals(HttpMethod.GET)) {
            URI uri = URI.create(request.absoluteURI());
            Http01Challenge challenge = challengeMap.get(uri);
            if (challenge != null) {
                request.response().end(challenge.getAuthorization());
            }
        }

    }
}
