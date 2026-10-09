package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Pregeneration progress as last seen on the wire. Capped by job count, and each entry carries the receive
 * time so the HUD can tell a live job from a server that stopped sending - progress frames only arrive while a
 * job ticks, so a stalled or crashed job otherwise leaves a frozen bar on screen forever.
 */
public final class IrisClientPregenState {
    /** Beyond this the panel reads as unreliable and the HUD mutes it. */
    public static final long STALE_AFTER_MILLIS = 5_000L;
    /** Beyond this the HUD stops drawing the panel at all. */
    public static final long EXPIRE_AFTER_MILLIS = 30_000L;
    static final int MAX_JOBS = 32;

    private final LongSupplier clock;
    private final LinkedHashMap<Long, Entry> jobs;
    private Long activeJobId;

    public IrisClientPregenState() {
        this(System::currentTimeMillis);
    }

    IrisClientPregenState(LongSupplier clock) {
        this.clock = clock;
        this.jobs = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, IrisClientPregenState.Entry> eldest) {
                return size() > MAX_JOBS;
            }
        };
        this.activeJobId = null;
    }

    public synchronized void onProgress(IrisMessage.PregenProgress progress) {
        jobs.put(progress.jobId(), new Entry(progress, clock.getAsLong()));
        activeJobId = progress.jobId();
    }

    public synchronized void onEnd(long jobId) {
        jobs.remove(jobId);
        Long current = activeJobId;
        if (current != null && current == jobId) {
            // Promote nothing: the server runs a single live job, so any remaining entry is
            // dead or orphaned — and the access-ordered map's head is the STALEST entry,
            // which is what the HUD used to promote.
            activeJobId = null;
        }
    }

    public synchronized IrisMessage.PregenProgress active() {
        Entry entry = activeEntry();
        return entry == null ? null : entry.progress();
    }

    /** Millis since the active job last reported, or -1 when there is no active job. */
    public synchronized long activeAgeMillis() {
        Entry entry = activeEntry();
        return entry == null ? -1L : Math.max(0L, clock.getAsLong() - entry.receivedAtMillis());
    }

    public synchronized boolean activeStale() {
        long age = activeAgeMillis();
        return age >= STALE_AFTER_MILLIS;
    }

    public synchronized boolean activeExpired() {
        long age = activeAgeMillis();
        return age >= EXPIRE_AFTER_MILLIS;
    }

    public synchronized Long activeJobId() {
        return activeJobId;
    }

    public synchronized int trackedJobs() {
        return jobs.size();
    }

    public synchronized void clear() {
        jobs.clear();
        activeJobId = null;
    }

    private Entry activeEntry() {
        Long current = activeJobId;
        if (current == null) {
            return null;
        }
        return jobs.get(current);
    }

    private record Entry(IrisMessage.PregenProgress progress, long receivedAtMillis) {
    }
}
