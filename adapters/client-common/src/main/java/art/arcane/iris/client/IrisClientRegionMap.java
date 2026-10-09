package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IrisClientRegionMap {
    private static final int MAX_REGIONS = 4096;

    private final ConcurrentHashMap<Long, Integer> states;
    private volatile boolean tracking;
    private volatile long jobId;
    private volatile Bounds bounds;

    public IrisClientRegionMap() {
        this.states = new ConcurrentHashMap<>();
        this.tracking = false;
        this.jobId = 0L;
        this.bounds = null;
    }

    public void onDelta(IrisMessage.PregenRegionDelta delta, Long activeJobId) {
        if (activeJobId == null) {
            return;
        }
        long active = activeJobId;
        if (delta.jobId() != active) {
            return;
        }
        if (!tracking || jobId != delta.jobId()) {
            resetTo(delta.jobId());
        }
        record(delta.regionX(), delta.regionZ(), delta.state());
    }

    public void onEnd(long endedJobId) {
        if (tracking && jobId == endedJobId) {
            clear();
        }
    }

    public boolean hasData() {
        return tracking && !states.isEmpty();
    }

    public Bounds bounds() {
        return bounds;
    }

    public void forEachCell(CellConsumer consumer) {
        for (Map.Entry<Long, Integer> entry : states.entrySet()) {
            long packed = entry.getKey();
            consumer.accept(regionX(packed), regionZ(packed), entry.getValue());
        }
    }

    public void clear() {
        states.clear();
        bounds = null;
        tracking = false;
        jobId = 0L;
    }

    private void resetTo(long newJobId) {
        states.clear();
        bounds = null;
        jobId = newJobId;
        tracking = true;
    }

    private void record(int regionX, int regionZ, int state) {
        long packed = pack(regionX, regionZ);
        if (!states.containsKey(packed)) {
            if (states.size() >= MAX_REGIONS) {
                return;
            }
            Bounds current = bounds;
            bounds = current == null ? new Bounds(regionX, regionZ, regionX, regionZ) : current.union(regionX, regionZ);
        }
        states.put(packed, state);
    }

    private static long pack(int regionX, int regionZ) {
        return ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);
    }

    private static int regionX(long packed) {
        return (int) (packed >> 32);
    }

    private static int regionZ(long packed) {
        return (int) packed;
    }

    @FunctionalInterface
    public interface CellConsumer {
        void accept(int regionX, int regionZ, int state);
    }

    public record Bounds(int minRegionX, int minRegionZ, int maxRegionX, int maxRegionZ) {
        public Bounds union(int regionX, int regionZ) {
            return new Bounds(
                    Math.min(minRegionX, regionX),
                    Math.min(minRegionZ, regionZ),
                    Math.max(maxRegionX, regionX),
                    Math.max(maxRegionZ, regionZ));
        }

        public int regionsWide() {
            return maxRegionX - minRegionX + 1;
        }

        public int regionsTall() {
            return maxRegionZ - minRegionZ + 1;
        }
    }
}
