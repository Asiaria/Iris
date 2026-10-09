package art.arcane.iris.api.terrain;

import java.util.Objects;

public record IrisWorldInfo(
        String dimensionKey,
        String worldIdentity,
        long seed,
        int minHeight,
        int maxHeight,
        int fluidHeight,
        boolean studio) {
    public IrisWorldInfo {
        Objects.requireNonNull(dimensionKey, "dimensionKey");
        Objects.requireNonNull(worldIdentity, "worldIdentity");
        if (maxHeight <= minHeight) {
            throw new IllegalArgumentException("maxHeight must exceed minHeight");
        }
    }

    public int height() {
        return maxHeight - minHeight;
    }
}
