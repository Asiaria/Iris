package art.arcane.iris.api.pregen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IrisPregenProgressTest {
    @Test
    public void anAbsentWorldNameFallsBackToTheIdentity() {
        IrisPregenProgress progress = new IrisPregenProgress(
                null, "minecraft:world", 12D, 1L, 2L, 1L, 0L, 3D, 4L, 5L, null, false);

        assertEquals("minecraft:world", progress.worldName());
        assertEquals("", progress.method());
    }

    @Test
    public void hostileNumbersAreNormalisedRatherThanPropagated() {
        IrisPregenProgress progress = new IrisPregenProgress(
                "world", "minecraft:world", 400D, -1L, -2L, -3L, -4L, -5D, -6L, -7L, "hybrid", true);

        assertEquals(100D, progress.percent(), 0D);
        assertEquals(0L, progress.generatedChunks());
        assertEquals(0L, progress.totalChunks());
        assertEquals(0L, progress.remainingChunks());
        assertEquals(0L, progress.failedChunks());
        assertEquals(0D, progress.chunksPerSecond(), 0D);
        assertEquals(0L, progress.etaMillis());
        assertEquals(0L, progress.elapsedMillis());
    }

    @Test
    public void nonFiniteRatesCollapseToZeroInsteadOfLeaking() {
        IrisPregenProgress nan = new IrisPregenProgress(
                "world", "minecraft:world", Double.NaN, 0L, 0L, 0L, 0L, Double.NaN, 0L, 0L, "hybrid", false);
        IrisPregenProgress infinite = new IrisPregenProgress(
                "world", "minecraft:world", Double.POSITIVE_INFINITY, 0L, 0L, 0L, 0L,
                Double.POSITIVE_INFINITY, 0L, 0L, "hybrid", false);

        assertEquals(0D, nan.percent(), 0D);
        assertEquals(0D, nan.chunksPerSecond(), 0D);
        assertEquals(0D, infinite.percent(), 0D);
        assertEquals(0D, infinite.chunksPerSecond(), 0D);
    }

    @Test(expected = NullPointerException.class)
    public void aProgressWithoutAWorldIdentityIsRejected() {
        new IrisPregenProgress("world", null, 0D, 0L, 0L, 0L, 0L, 0D, 0L, 0L, "hybrid", false);
    }
}
