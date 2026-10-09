package art.arcane.iris.api.tree;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TreeFellerOptionsTest {
    @Test
    public void factoriesExposeStableAccessModes() {
        TreeFellerOptions standalone = TreeFellerOptions.standalone();
        TreeFellerOptions override = TreeFellerOptions.integrationOverride(75, TreeFellerRunHooks.NONE);

        assertEquals(TreeFellerAccess.STANDALONE, standalone.access());
        assertEquals(0, standalone.durabilityPreservationChance());
        assertEquals(TreeFellerAccess.INTEGRATION_OVERRIDE, override.access());
        assertEquals(75, override.durabilityPreservationChance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void chanceBelowRangeIsRejected() {
        TreeFellerOptions.integrationOverride(-1, TreeFellerRunHooks.NONE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void chanceAboveRangeIsRejected() {
        TreeFellerOptions.integrationOverride(101, TreeFellerRunHooks.NONE);
    }
}
