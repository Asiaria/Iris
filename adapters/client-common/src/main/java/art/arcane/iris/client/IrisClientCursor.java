package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;
import art.arcane.iris.spi.protocol.IrisMessageCodec;

import java.util.function.LongSupplier;

public final class IrisClientCursor {
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 500L;
    private static final long SAME_POSITION_REFRESH_MILLIS = 2_000L;

    private final ClientPacketSink sink;
    private final LongSupplier clock;
    private volatile IrisMessage.CursorInfo latest;
    private int lastRequestedX;
    private int lastRequestedZ;
    private boolean requested;
    private long lastRequestMillis;

    public IrisClientCursor(ClientPacketSink sink, LongSupplier clock) {
        this.sink = sink;
        this.clock = clock;
        this.latest = null;
        this.lastRequestedX = 0;
        this.lastRequestedZ = 0;
        this.requested = false;
        this.lastRequestMillis = 0L;
    }

    public synchronized void requestFor(int blockX, int blockZ) {
        long now = clock.getAsLong();
        boolean samePosition = requested && blockX == lastRequestedX && blockZ == lastRequestedZ;
        if (samePosition && now - lastRequestMillis < SAME_POSITION_REFRESH_MILLIS) {
            return;
        }
        if (now - lastRequestMillis < MIN_REQUEST_INTERVAL_MILLIS) {
            return;
        }
        sink.send(IrisMessageCodec.encode(new IrisMessage.CursorInfoRequest(blockX, blockZ)));
        lastRequestedX = blockX;
        lastRequestedZ = blockZ;
        requested = true;
        lastRequestMillis = now;
    }

    public void onCursorInfo(IrisMessage.CursorInfo info) {
        latest = info;
    }

    public IrisMessage.CursorInfo latest() {
        return latest;
    }

    public synchronized void clear() {
        latest = null;
        requested = false;
        lastRequestMillis = 0L;
    }
}
