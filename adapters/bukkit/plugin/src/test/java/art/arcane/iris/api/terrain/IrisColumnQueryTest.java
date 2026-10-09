package art.arcane.iris.api.terrain;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IrisColumnQueryTest {
    @Test
    public void fieldsAreCopiedOnBothSidesOfTheBoundary() {
        EnumSet<IrisColumnField> supplied = EnumSet.of(IrisColumnField.SURFACE_KIND);
        IrisColumnQuery query = IrisColumnQuery.rect(0, 0, 15, 15, 1, supplied);

        supplied.add(IrisColumnField.BIOME_KEY);
        assertEquals(EnumSet.of(IrisColumnField.SURFACE_KIND), query.fields());

        EnumSet<IrisColumnField> returned = query.fields();
        returned.add(IrisColumnField.SURFACE_HEIGHT);
        assertEquals(EnumSet.of(IrisColumnField.SURFACE_KIND), query.fields());
    }

    @Test
    public void columnCountAndChunkCountAreDecoupledByStride() {
        IrisColumnQuery query = IrisColumnQuery.rect(
                0, 0, 6399, 6399, 64, EnumSet.of(IrisColumnField.SURFACE_KIND));

        assertEquals(10_000L, query.columnCount());
        assertEquals(160_000L, query.chunkCount());
        assertTrue("chunk span must dominate the column count for a strided query",
                query.chunkCount() > query.columnCount());
    }

    @Test
    public void countsAreExactForASingleChunk() {
        IrisColumnQuery query = IrisColumnQuery.rect(
                0, 0, 15, 15, 1, EnumSet.of(IrisColumnField.SURFACE_HEIGHT));

        assertEquals(256L, query.columnCount());
        assertEquals(1L, query.chunkCount());
    }

    @Test
    public void countsSurviveNegativeCoordinates() {
        IrisColumnQuery query = IrisColumnQuery.rect(
                -32, -32, -1, -1, 8, EnumSet.of(IrisColumnField.SURFACE_HEIGHT));

        assertEquals(16L, query.columnCount());
        assertEquals(4L, query.chunkCount());
    }

    @Test
    public void countsSaturateInsteadOfWrappingNegativePastTheCaps() {
        IrisColumnQuery query = IrisColumnQuery.rect(
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1,
                EnumSet.of(IrisColumnField.SURFACE_HEIGHT));

        assertEquals(Long.MAX_VALUE, query.columnCount());
        assertTrue(query.chunkCount() > 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyFieldSetIsRejected() {
        IrisColumnQuery.rect(0, 0, 15, 15, 1, EnumSet.noneOf(IrisColumnField.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invertedBoundsAreRejected() {
        IrisColumnQuery.rect(16, 0, 0, 15, 1, EnumSet.of(IrisColumnField.SURFACE_KIND));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroStrideIsRejected() {
        IrisColumnQuery.rect(0, 0, 15, 15, 0, EnumSet.of(IrisColumnField.SURFACE_KIND));
    }
}
