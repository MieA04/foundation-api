package org.miea04.foundation.runtime.capability;

import org.miea04.foundation.api.capability.*;
import org.miea04.foundation.api.exception.CapabilityRegisterRepeatException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CapabilityRegistryImpl
 *
 * @author MieMie
 */
public class CapabilityRegistry implements org.miea04.foundation.api.capability.CapabilityRegistry {

    private final Map<CapabilityKey, CapabilityInfo> capability = new ConcurrentHashMap<>();

    @Override
    public void register(CapabilityKey key, CapabilityInfo info) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(info);

        Object old = capability.putIfAbsent(key, info);

        if (old != null) {
            throw new CapabilityRegisterRepeatException(key.namespace());
        }
    }

    @Override
    public void update(CapabilityKey key, CapabilityInfo info) {
        this.capability.replace(key, info);
    }

    @Override
    public Optional<CapabilityInfo> get(CapabilityKey key) {
        return Optional.ofNullable(this.capability.get(key));
    }

    @Override
    public boolean contains(CapabilityKey key) {
        return this.capability.containsKey(key);
    }

    @Override
    public boolean isAvailable(CapabilityKey key) {
        CapabilityInfo capabilityInfo = this.capability.get(key);
        if (capabilityInfo == null) {
            return false;
        }

        return capabilityInfo.status() == CapabilityStatus.AVAILABLE;
    }

    @Override
    public List<CapabilityEntry> list() {
        return this.capability
                .entrySet()
                .stream()
                .map(
                        entry -> new CapabilityEntry(
                                entry.getKey(), entry.getValue()
                        )
                )
                .toList();
    }
}
