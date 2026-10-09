package art.arcane.iris.modded;

import art.arcane.iris.modded.service.ModdedService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class ModdedServiceManagerTest {
    @Test
    public void enablesAndDisablesEveryRegisteredService() {
        ModdedServiceManager manager = new ModdedServiceManager();
        FirstService first = manager.register(FirstService.class, new FirstService());
        SecondService second = manager.register(SecondService.class, new SecondService(null, null));

        manager.enableAll();
        manager.disableAll();

        assertEquals(1, first.enableCount);
        assertEquals(1, first.disableCount);
        assertEquals(1, second.enableCount);
        assertEquals(1, second.disableCount);
    }

    @Test
    public void rollsBackEnabledServicesAndPreservesCleanupFailures() {
        ModdedServiceManager manager = new ModdedServiceManager();
        FirstService first = manager.register(FirstService.class, new FirstService());
        RuntimeException original = new RuntimeException("enable failed");
        RuntimeException cleanup = new RuntimeException("cleanup failed");
        SecondService second = manager.register(
                SecondService.class, new SecondService(original, cleanup));

        RuntimeException thrown = assertThrows(RuntimeException.class, manager::enableAll);

        assertSame(original, thrown);
        assertEquals(1, first.enableCount);
        assertEquals(1, first.disableCount);
        assertEquals(1, second.enableCount);
        assertEquals(1, second.disableCount);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanup, thrown.getSuppressed()[0]);

        manager.rollback(thrown);
        assertNull(manager.service(FirstService.class));
        assertNull(manager.service(SecondService.class));
    }

    @Test
    public void disableAttemptsEveryServiceInReverseOrderAndNeverRethrows() {
        ModdedServiceManager manager = new ModdedServiceManager();
        RuntimeException firstFailure = new RuntimeException("first disable failed");
        RuntimeException secondFailure = new RuntimeException("second disable failed");
        FirstService first = manager.register(FirstService.class, new FirstService());
        first.disableFailure = firstFailure;
        SecondService second = manager.register(
                SecondService.class, new SecondService(null, secondFailure));

        manager.enableAll();
        manager.disableAll();

        assertEquals(1, first.disableCount);
        assertEquals(1, second.disableCount);
        assertEquals(1, secondFailure.getSuppressed().length);
        assertSame(firstFailure, secondFailure.getSuppressed()[0]);

        manager.disableAll();
        assertEquals(1, first.disableCount);
        assertEquals(1, second.disableCount);
    }

    private static final class FirstService implements ModdedService {
        private RuntimeException disableFailure;
        private int enableCount;
        private int disableCount;

        @Override
        public void onEnable() {
            enableCount++;
        }

        @Override
        public void onDisable() {
            disableCount++;
            if (disableFailure != null) {
                throw disableFailure;
            }
        }
    }

    private static final class SecondService implements ModdedService {
        private final RuntimeException enableFailure;
        private final RuntimeException disableFailure;
        private int enableCount;
        private int disableCount;

        private SecondService(RuntimeException enableFailure, RuntimeException disableFailure) {
            this.enableFailure = enableFailure;
            this.disableFailure = disableFailure;
        }

        @Override
        public void onEnable() {
            enableCount++;
            if (enableFailure != null) {
                throw enableFailure;
            }
        }

        @Override
        public void onDisable() {
            disableCount++;
            if (disableFailure != null) {
                throw disableFailure;
            }
        }
    }
}
