package org.miea04.foundation.runtime.capability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.miea04.foundation.api.capability.CapabilityEntry;
import org.miea04.foundation.api.capability.CapabilityInfo;
import org.miea04.foundation.api.capability.CapabilityKey;
import org.miea04.foundation.api.capability.CapabilityStatus;
import org.miea04.foundation.api.exception.CapabilityRegisterRepeatException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CapabilityRegistryTest {

    @Test
    void registerStoresCapabilityAndListsEntry() {
        CapabilityRegistry registry = new CapabilityRegistry();
        CapabilityKey key = CapabilityKey.of("foundation", "inventory");
        CapabilityInfo info = capabilityInfo(CapabilityStatus.AVAILABLE, "storage", "query");

        registry.register(key, info);

        assertTrue(registry.contains(key));
        assertEquals(Optional.of(info), registry.get(key));
        assertTrue(registry.isAvailable(key));
        assertEquals(List.of(new CapabilityEntry(key, info)), registry.list());
    }

    @Test
    void duplicateRegisterThrowsAndKeepsOriginalCapability() {
        CapabilityRegistry registry = new CapabilityRegistry();
        CapabilityKey key = CapabilityKey.of("foundation", "config");
        CapabilityInfo first = capabilityInfo(CapabilityStatus.REGISTERED, "read");
        CapabilityInfo second = capabilityInfo(CapabilityStatus.AVAILABLE, "write");

        registry.register(key, first);

        CapabilityRegisterRepeatException exception = assertThrows(
                CapabilityRegisterRepeatException.class,
                () -> registry.register(key, second)
        );

        assertTrue(exception.getMessage().contains(key.namespace()));
        assertEquals(Optional.of(first), registry.get(key));
    }

    @Test
    void updateExistingCapabilityChangesAvailability() {
        CapabilityRegistry registry = new CapabilityRegistry();
        CapabilityKey key = CapabilityKey.of("foundation", "network");
        CapabilityInfo initializing = capabilityInfo(CapabilityStatus.INITIALIZING, "connect");
        CapabilityInfo available = capabilityInfo(CapabilityStatus.AVAILABLE, "connect", "sync");

        registry.register(key, initializing);
        registry.update(key, available);

        assertEquals(Optional.of(available), registry.get(key));
        assertTrue(registry.isAvailable(key));
    }

    @Test
    void missingCapabilityIsNotAvailableAndUpdateDoesNotCreateIt() {
        CapabilityRegistry registry = new CapabilityRegistry();
        CapabilityKey key = CapabilityKey.of("missing", "capability");

        registry.update(key, capabilityInfo(CapabilityStatus.AVAILABLE, "ghost"));

        assertFalse(registry.contains(key));
        assertTrue(registry.get(key).isEmpty());
        assertFalse(registry.isAvailable(key));
    }

    @Test
    void registerRejectsNullInputs() {
        CapabilityRegistry registry = new CapabilityRegistry();
        CapabilityKey key = CapabilityKey.of("foundation", "null-check");
        CapabilityInfo info = capabilityInfo(CapabilityStatus.REGISTERED, "check");

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> registry.register(null, info)),
                () -> assertThrows(NullPointerException.class, () -> registry.register(key, null))
        );
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void concurrentDuplicateRegisterAllowsOnlyOneWinner() throws InterruptedException {
        CapabilityRegistry registry = new CapabilityRegistry();
        CapabilityKey key = CapabilityKey.of("foundation", "single-writer");
        int workers = 32;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for (int i = 0; i < workers; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    registry.register(key, capabilityInfo(CapabilityStatus.AVAILABLE, "worker-" + index));
                    success.incrementAndGet();
                } catch (CapabilityRegisterRepeatException exception) {
                    duplicate.incrementAndGet();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(ready.await(1, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

        assertEquals(1, success.get());
        assertEquals(workers - 1, duplicate.get());
        assertTrue(registry.get(key).isPresent());
    }

    private static CapabilityInfo capabilityInfo(CapabilityStatus status, String... features) {
        return new CapabilityInfo(status, "test-mod", "1.0.0", "test", Set.of(features));
    }
}
