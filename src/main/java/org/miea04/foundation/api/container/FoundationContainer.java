package org.miea04.foundation.api.container;

import org.miea04.foundation.api.container.service.ServiceEntry;

import java.util.Optional;

/**
 * FoundationContainer
 *
 * @author MieMie
 */
public interface FoundationContainer {
    <T> void registry(Class<?> type, ServiceEntry<T> entry);

    <T> Optional<T> get(Class<T> key);

    Optional<ServiceEntry<?>> getEntry(Class<?> key);

    boolean contains(Class<?> type);
}
