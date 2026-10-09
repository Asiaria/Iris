package art.arcane.iris.api.terrain;

import java.util.EnumSet;
import java.util.Objects;

public record IrisColumnQuery(
        int minBlockX,
        int minBlockZ,
        int maxBlockX,
        int maxBlockZ,
        int strideBlocks,
        EnumSet<IrisColumnField> fields) {
    public IrisColumnQuery {
        Objects.requireNonNull(fields, "fields");
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("at least one field is required");
        }
        if (maxBlockX < minBlockX || maxBlockZ < minBlockZ) {
            throw new IllegalArgumentException("query bounds are inverted");
        }
        if (strideBlocks < 1) {
            throw new IllegalArgumentException("strideBlocks must be at least 1");
        }
        fields = EnumSet.copyOf(fields);
    }

    public static IrisColumnQuery rect(
            int minBlockX,
            int minBlockZ,
            int maxBlockX,
            int maxBlockZ,
            int strideBlocks,
            EnumSet<IrisColumnField> fields) {
        return new IrisColumnQuery(minBlockX, minBlockZ, maxBlockX, maxBlockZ, strideBlocks, fields);
    }

    public long columnCount() {
        long columnsX = (((long) maxBlockX - (long) minBlockX) / strideBlocks) + 1L;
        long columnsZ = (((long) maxBlockZ - (long) minBlockZ) / strideBlocks) + 1L;
        return saturatedProduct(columnsX, columnsZ);
    }

    public long chunkCount() {
        long chunksX = ((long) (maxBlockX >> 4) - (long) (minBlockX >> 4)) + 1L;
        long chunksZ = ((long) (maxBlockZ >> 4) - (long) (minBlockZ >> 4)) + 1L;
        return saturatedProduct(chunksX, chunksZ);
    }

    private static long saturatedProduct(long left, long right) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public EnumSet<IrisColumnField> fields() {
        return EnumSet.copyOf(fields);
    }
}
