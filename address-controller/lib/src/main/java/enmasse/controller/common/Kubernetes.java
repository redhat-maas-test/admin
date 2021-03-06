package enmasse.controller.common;

import enmasse.controller.address.DestinationCluster;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.util.*;

/**
 * Interface for Kubernetes operations done by the address controller
 */
public interface Kubernetes {

    static String sanitizeName(String name) {
        String replaced = name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        if (replaced.startsWith("-")) {
            replaced = replaced.replaceFirst("-", "1");
        }
        if (replaced.endsWith("-")) {
            replaced = replaced.substring(0, replaced.length() - 2) + "1";
        }
        return replaced;
    }

    static void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            if (labels == null) {
                labels = new LinkedHashMap<>();
            }
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }

    static void addObjectAnnotation(KubernetesList items, String annotationKey, String annotationValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> annotations = item.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new LinkedHashMap<>();
            }
            annotations.put(annotationKey, annotationValue);
            item.getMetadata().setAnnotations(annotations);
        }
    }

    InstanceId getInstanceId();
    Kubernetes withInstance(InstanceId instance);

    List<DestinationCluster> listClusters();
    void create(HasMetadata ... resources);
    void create(KubernetesList resources);
    void delete(KubernetesList resources);
    void delete(HasMetadata ... resources);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    Namespace createNamespace(InstanceId instance);

    void deleteNamespace(String namespace);

    void addDefaultViewPolicy(InstanceId instance);

    boolean hasService(String service);
    void createInstanceSecret(String secretName, InstanceId instanceId);

    Set<Deployment> getReadyDeployments();

    boolean isDestinationClusterReady(String clusterId);

    List<Namespace> listNamespaces(Map<String, String> labels);

    List<Pod> listRouters();
}
