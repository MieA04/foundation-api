package org.miea04.foundation.performance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.miea04.foundation.api.capability.CapabilityInfo;
import org.miea04.foundation.api.capability.CapabilityKey;
import org.miea04.foundation.api.capability.CapabilityStatus;
import org.miea04.foundation.api.container.serivce.ServiceEntry;
import org.miea04.foundation.api.container.serivce.ServiceInfo;
import org.miea04.foundation.api.container.serivce.ServiceStatus;
import org.miea04.foundation.runtime.capability.CapabilityRegistry;
import org.miea04.foundation.runtime.container.DefaultContainer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("performance")
class FoundationPerformanceLimitTest {

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void capabilityRegistryHandlesLargeRegistrationAndLookupVolume() {
        CapabilityRegistry registry = new CapabilityRegistry();
        int count = 50_000;

        for (int i = 0; i < count; i++) {
            registry.register(
                    CapabilityKey.of("load-test", "capability-" + i),
                    new CapabilityInfo(CapabilityStatus.AVAILABLE, "load-test", "1.0.0", "limit", Set.of("feature-" + i))
            );
        }

        assertEquals(count, registry.list().size());
        for (int i = 0; i < count; i++) {
            CapabilityKey key = CapabilityKey.of("load-test", "capability-" + i);
            assertTrue(registry.contains(key));
            assertTrue(registry.isAvailable(key));
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void containerHandlesHighFrequencyLookupVolume() {
        DefaultContainer container = new DefaultContainer();
        LookupService service = new LookupService();

        container.registry(
                LookupService.class,
                new ServiceEntry<>(
                        LookupService.class,
                        service,
                        new ServiceInfo("load-test", "1.0.0", ServiceStatus.AVAILABLE, "limit", Set.of(), Map.of())
                )
        );

        int lookups = 1_000_000;
        for (int i = 0; i < lookups; i++) {
            assertSame(service, container.get(LookupService.class).orElseThrow());
        }
    }

    private static final class LookupService {
    }
}
