package org.miea04.foundation.api.capability;

import java.util.List;
import java.util.Optional;

/**
 * CapabilityRegistry
 *
 * @author MieMie
 */
public interface CapabilityRegistry {

    void register(CapabilityKey key, CapabilityInfo info);

    void update(CapabilityKey key, CapabilityInfo info);

    Optional<CapabilityInfo> get(CapabilityKey key);

    boolean contains(CapabilityKey key);

    boolean isAvailable(CapabilityKey key);

    List<CapabilityEntry> list();

}
