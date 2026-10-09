package art.arcane.iris.api.pregen;

import java.util.Objects;

public record IrisPregenProgress(
        String worldName,
        String worldIdentity,
        double percent,
        long generatedChunks,
        long totalChunks,
        long remainingChunks,
        long failedChunks,
        double chunksPerSecond,
        long etaMillis,
        long elapsedMillis,
        String method,
        boolean paused) {
    public IrisPregenProgress {
        Objects.requireNonNull(worldIdentity, "worldIdentity");
        worldName = worldName == null ? worldIdentity : worldName;
        method = method == null ? "" : method;
        percent = Double.isFinite(percent) ? Math.clamp(percent, 0D, 100D) : 0D;
        generatedChunks = Math.max(0L, generatedChunks);
        totalChunks = Math.max(0L, totalChunks);
        remainingChunks = Math.max(0L, remainingChunks);
        failedChunks = Math.max(0L, failedChunks);
        chunksPerSecond = Double.isFinite(chunksPerSecond) ? Math.max(0D, chunksPerSecond) : 0D;
        etaMillis = Math.max(0L, etaMillis);
        elapsedMillis = Math.max(0L, elapsedMillis);
    }
}
