package art.arcane.iris.modded.service;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModdedTreeFellerServiceTest {
    @Test
    public void syntheticBreakProbeDepthIsNestedAndRestored() {
        assertFalse(ModdedTreeFellerService.isBreakProbe());

        boolean accepted = ModdedTreeFellerService.runBreakProbe(() -> {
            assertTrue(ModdedTreeFellerService.isBreakProbe());
            return ModdedTreeFellerService.runBreakProbe(() -> {
                assertTrue(ModdedTreeFellerService.isBreakProbe());
                return true;
            });
        });

        assertTrue(accepted);
        assertFalse(ModdedTreeFellerService.isBreakProbe());
    }

    @Test
    public void syntheticBreakProbeDepthIsRestoredAfterFailure() {
        try {
            ModdedTreeFellerService.runBreakProbe(() -> {
                throw new IllegalStateException("probe failure");
            });
            fail("Expected probe failure");
        } catch (IllegalStateException expected) {
            assertFalse(ModdedTreeFellerService.isBreakProbe());
        }
    }
}
