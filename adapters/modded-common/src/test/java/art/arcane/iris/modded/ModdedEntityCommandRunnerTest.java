package art.arcane.iris.modded;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ModdedEntityCommandRunnerTest {
    @Test
    public void commandPreparationRemovesOneLeadingSlashAndExpandsCoordinates() {
        String command = ModdedEntityCommandRunner.prepareCommand(
                "/summon pig {x} {y} {z} {x}", -12, 65, 99);

        assertEquals("summon pig -12 65 99 -12", command);
    }

    @Test
    public void blankCommandsAreIgnored() {
        assertNull(ModdedEntityCommandRunner.prepareCommand("  ", 0, 0, 0));
        assertNull(ModdedEntityCommandRunner.prepareCommand(null, 0, 0, 0));
    }

    @Test
    public void delaysMatchPaperClampingRules() {
        assertEquals(0, ModdedEntityCommandRunner.clampDelay(-10L, 0));
        assertEquals(1, ModdedEntityCommandRunner.clampDelay(-10L, 1));
        assertEquals(73, ModdedEntityCommandRunner.clampDelay(73L, 0));
        assertEquals(Integer.MAX_VALUE, ModdedEntityCommandRunner.clampDelay(Long.MAX_VALUE, 0));
    }
}
