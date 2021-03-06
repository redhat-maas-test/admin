package enmasse.controller.common;

import io.fabric8.kubernetes.client.*;

import java.util.Set;

/**
 * Interface for components supporting listing and watching a resource.
 */
public interface Resource<T> {
    io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher watcher);
    Set<T> listResources();
}
