package org.miea04.foundation.runtime.container;

import org.miea04.foundation.api.container.FoundationContainer;
import org.miea04.foundation.api.exception.ServiceAlreadyRegisteredException;
import org.miea04.foundation.api.container.serivce.ServiceEntry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultContainer
 *
 * @author MieMie
 */
public class DefaultContainer implements FoundationContainer {
    private final Map<Class<?>, ServiceEntry<?>> container = new ConcurrentHashMap<>();

    @Override
    public <T> void registry(Class<?> type, ServiceEntry<T> entry) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(entry);

        Object old = container.putIfAbsent(type, entry);

        if (old != null) {
            throw new ServiceAlreadyRegisteredException(type.getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type) {
        ServiceEntry<?> entry = this.container.get(type);
        if (entry == null || !entry.isAvailable()) {
            return Optional.empty();
        }

        return Optional.of((T) entry.instance());
    }

    @Override
    public Optional<ServiceEntry<?>> getEntry(Class<?> key) {
        ServiceEntry<?> entry = this.container.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        return Optional.of(entry);
    }

    @Override
    public boolean contains(Class<?> type) {
        return container.containsKey(type);
    }
}
