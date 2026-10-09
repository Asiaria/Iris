package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IrisClientDimensionTest {
    @Test
    public void packSeedAndHeightChangesInvalidateDimensionCaches() {
        IrisClientDimension dimension = new IrisClientDimension();
        IrisMessage.DimensionStatus initial = new IrisMessage.DimensionStatus(
                "minecraft:overworld", "overworld", 1L, -64, 320, true);

        assertTrue(dimension.onDimensionStatus(initial));
        assertFalse(dimension.onDimensionStatus(initial));
        assertTrue(dimension.onDimensionStatus(new IrisMessage.DimensionStatus(
                "minecraft:overworld", "other", 1L, -64, 320, true)));
        assertTrue(dimension.onDimensionStatus(new IrisMessage.DimensionStatus(
                "minecraft:overworld", "other", 2L, -64, 320, true)));
        assertTrue(dimension.onDimensionStatus(new IrisMessage.DimensionStatus(
                "minecraft:overworld", "other", 2L, 0, 256, true)));
    }
}
