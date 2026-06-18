package org.miea04.foundation.runtime.container;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.miea04.foundation.api.container.serivce.ServiceEntry;
import org.miea04.foundation.api.container.serivce.ServiceInfo;
import org.miea04.foundation.api.container.serivce.ServiceStatus;
import org.miea04.foundation.api.exception.ServiceAlreadyRegisteredException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DefaultContainerTest {

    @Test
    void registryStoresAvailableServiceAndEntry() {
        DefaultContainer container = new DefaultContainer();
        TestService service = new TestService("primary");
        ServiceEntry<TestService> entry = serviceEntry(TestService.class, service, ServiceStatus.AVAILABLE);

        container.registry(TestService.class, entry);

        assertTrue(container.contains(TestService.class));
        assertEquals(Optional.of(service), container.get(TestService.class));
        assertEquals(Optional.of(entry), container.getEntry(TestService.class));
    }

    @Test
    void getReturnsDegradedServiceButHidesUnavailableService() {
        DefaultContainer container = new DefaultContainer();
        TestService degraded = new TestService("degraded");
        OtherService unavailable = new OtherService();

        container.registry(TestService.class, serviceEntry(TestService.class, degraded, ServiceStatus.DEGRADED));
        container.registry(OtherService.class, serviceEntry(OtherService.class, unavailable, ServiceStatus.UNAVAILABLE));

        assertEquals(Optional.of(degraded), container.get(TestService.class));
        assertTrue(container.get(OtherService.class).isEmpty());
        assertTrue(container.getEntry(OtherService.class).isPresent());
    }

    @Test
    void duplicateRegistryThrowsAndKeepsOriginalService() {
        DefaultContainer container = new DefaultContainer();
        TestService first = new TestService("first");
        TestService second = new TestService("second");

        container.registry(TestService.class, serviceEntry(TestService.class, first, ServiceStatus.AVAILABLE));

        ServiceAlreadyRegisteredException exception = assertThrows(
                ServiceAlreadyRegisteredException.class,
                () -> container.registry(TestService.class, serviceEntry(TestService.class, second, ServiceStatus.AVAILABLE))
        );

        assertTrue(exception.getMessage().contains(TestService.class.getName()));
        assertEquals(Optional.of(first), container.get(TestService.class));
    }

    @Test
    void registryRejectsNullInputs() {
        DefaultContainer container = new DefaultContainer();
        ServiceEntry<TestService> entry = serviceEntry(TestService.class, new TestService("service"), ServiceStatus.AVAILABLE);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> container.registry(null, entry)),
                () -> assertThrows(NullPointerException.class, () -> container.registry(TestService.class, null))
        );
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void concurrentDuplicateRegistryAllowsOnlyOneWinner() throws InterruptedException {
        DefaultContainer container = new DefaultContainer();
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
                    container.registry(
                            TestService.class,
                            serviceEntry(TestService.class, new TestService("service-" + index), ServiceStatus.AVAILABLE)
                    );
                    success.incrementAndGet();
                } catch (ServiceAlreadyRegisteredException exception) {
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
        assertTrue(container.get(TestService.class).isPresent());
    }

    private static <T> ServiceEntry<T> serviceEntry(Class<T> type, T instance, ServiceStatus status) {
        return new ServiceEntry<>(
                type,
                instance,
                new ServiceInfo("test-mod", "1.0.0", status, "test", Set.of("test"), Map.of("env", "test"))
        );
    }

    private record TestService(String name) {
    }

    private static final class OtherService {
    }
}
