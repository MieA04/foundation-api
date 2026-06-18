package org.miea04.foundation.api.container.serivce;

import java.util.Map;
import java.util.Set;

/**
 * ServiceInfo
 *
 * @author MieMie
 */
public record ServiceInfo(
        String providerModId,
        String providerVersion,
        ServiceStatus status,
        String reason,
        Set<String> tags,
        Map<String, String> metadata
) {}
