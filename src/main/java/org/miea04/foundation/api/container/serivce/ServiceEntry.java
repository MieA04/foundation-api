package org.miea04.foundation.api.container.serivce;

/**
 * ServiceEntry
 *
 * @author MieMie
 */
public record ServiceEntry<T>(
        Class<T> type,
        T instance,
        ServiceInfo info
) {
    public boolean isAvailable() {
        return info.status() == ServiceStatus.AVAILABLE
                || info.status() == ServiceStatus.DEGRADED;
    }
}
