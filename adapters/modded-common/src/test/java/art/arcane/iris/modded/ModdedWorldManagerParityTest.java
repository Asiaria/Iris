package art.arcane.iris.modded;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModdedWorldManagerParityTest {
    @Test
    public void ambientChunkReservoirFillsBeforeSampling() {
        assertEquals(0, ModdedWorldManager.reservoirSlot(0, 4, 0));
        assertEquals(3, ModdedWorldManager.reservoirSlot(3, 4, 0));
    }

    @Test
    public void ambientChunkReservoirReplacesOnlyOnInRangeRolls() {
        assertEquals(2, ModdedWorldManager.reservoirSlot(4, 4, 2));
        assertEquals(0, ModdedWorldManager.reservoirSlot(9, 4, 0));
        assertEquals(-1, ModdedWorldManager.reservoirSlot(4, 4, 4));
        assertEquals(-1, ModdedWorldManager.reservoirSlot(9, 4, 7));
    }

    @Test
    public void normalWorldAlwaysAllowsEntitySpawning() {
        assertTrue(ModdedWorldManager.entitySpawningEnabled(false, false));
        assertTrue(ModdedWorldManager.entitySpawningEnabled(false, true));
    }

    @Test
    public void studioWorldRequiresItsEntitySpawningSetting() {
        assertFalse(ModdedWorldManager.entitySpawningEnabled(true, false));
        assertTrue(ModdedWorldManager.entitySpawningEnabled(true, true));
    }
}
