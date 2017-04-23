package enmasse.controller.instance;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.controller.common.OpenShift;
import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InstanceController extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(InstanceController.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final OpenShift openShift;
    private final InstanceManager instanceManager;
    private volatile Watch watch;

    public InstanceController(OpenShift openShift, InstanceManager instanceManager) {
        this.openShift = openShift;
        this.instanceManager  = instanceManager;
    }

    @Override
    public void start() {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("app", "enmasse");
        labelMap.put("type", "instance");
        vertx.executeBlocking((Future<Watch> promise) -> {
                try {
                    promise.complete(openShift.watchConfigMaps(openShift.getInstanceId().getNamespace(), labelMap, this));
                } catch (Exception e) {
                    promise.fail(e);
                }
        }, result -> {
            if (result.succeeded()) {
                this.watch = result.result();
            } else {
                log.error("Error starting watch", result.cause());
            }
        });
    }

    @Override
    public void stop() {
        if (watch != null) {
            watch.close();
        }
    }


    @Override
    public void eventReceived(Action action, ConfigMap resource) {
        switch (action) {
            case ADDED:
                toInstance(resource).ifPresent(instanceManager::create);
            case DELETED:
                toInstance(resource).ifPresent(instanceManager::delete);
            case MODIFIED:
                log.info("Instance updated... ignoring");
                break;
            case ERROR:
                log.warn("Error event: " + action);
                break;
        }

    }

    private Optional<Instance> toInstance(ConfigMap resource) {
        try {
            return Optional.of(mapper.readValue(resource.getData().get("instance"), enmasse.controller.api.v3.Instance.class).getInstance());
        } catch (IOException e) {
            log.warn("Error deserializing instance", e);
            return Optional.empty();
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        log.error("Client closed", cause);
    }
}
