package art.arcane.iris.api.terrain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IrisWorldInfoTest {
    @Test
    public void heightIsDerivedFromTheAbsoluteBounds() {
        IrisWorldInfo info = new IrisWorldInfo("overworld", "minecraft:world", 42L, -64, 320, 63, false);

        assertEquals(384, info.height());
        assertEquals(63, info.fluidHeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void collapsedHeightRangeIsRejected() {
        new IrisWorldInfo("overworld", "minecraft:world", 42L, 0, 0, 63, false);
    }

    @Test(expected = NullPointerException.class)
    public void nullDimensionKeyIsRejected() {
        new IrisWorldInfo(null, "minecraft:world", 42L, -64, 320, 63, false);
    }

    @Test(expected = NullPointerException.class)
    public void nullWorldIdentityIsRejected() {
        new IrisWorldInfo("overworld", null, 42L, -64, 320, 63, false);
    }
}
