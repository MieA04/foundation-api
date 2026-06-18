package org.miea04.foundation.api.capability;

import java.util.Set;

/**
 * CapabilityInfo
 *
 * @author MieMie
 */
public record CapabilityInfo(
        CapabilityStatus status,
        String providerModId,
        String providerVersion,
        String reason,
        Set<String> features
) {}
