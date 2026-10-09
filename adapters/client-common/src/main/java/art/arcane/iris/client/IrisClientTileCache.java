package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;
import art.arcane.iris.spi.protocol.IrisMessageCodec;
import art.arcane.iris.spi.protocol.IrisProtocol;
import art.arcane.iris.spi.protocol.ProtocolException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

public final class IrisClientTileCache {
    /** Enough for a 1080p viewport plus a ring of prefetch. 128x128 ARGB tiles are 64KB each. */
    static final int DEFAULT_MAX_CACHED_TILES = 256;
    /** Ceiling for {@link #ensureCapacity(int)}: 4K at scale 1 needs ~660 tiles, so ~64MB of tile images. */
    static final int ABSOLUTE_MAX_CACHED_TILES = 2048;
    private static final long REQUEST_RETRY_MILLIS = 3000L;
    private static final int REQUESTS_PER_SECOND = IrisProtocol.MAX_VISION_TILE_REQUESTS_PER_SECOND;

    private final ClientPacketSink sink;
    private final LongSupplier clock;
    private final IrisTileAssembler assembler;
    private final LinkedHashMap<IrisTileKey, IrisTileImage> cache;
    private final Map<IrisTileKey, Long> pending;
    private final Deque<IrisTileKey> queue;
    private final Set<IrisTileKey> queued;
    private int capacity;
    private long windowStartMillis;
    private int sentInWindow;
    private long droppedMalformed;

    public IrisClientTileCache(ClientPacketSink sink, LongSupplier clock) {
        this.sink = sink;
        this.clock = clock;
        this.assembler = new IrisTileAssembler();
        this.capacity = DEFAULT_MAX_CACHED_TILES;
        this.cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IrisTileKey, IrisTileImage> eldest) {
                return size() > capacity;
            }
        };
        this.pending = new HashMap<>();
        this.queue = new ArrayDeque<>();
        this.queued = new HashSet<>();
        this.windowStartMillis = 0L;
        this.sentInWindow = 0;
        this.droppedMalformed = 0L;
    }

    /**
     * Raises the retained-tile budget to cover what the viewport can show at once. Without this a 4K screen at
     * scale 1 evicts tiles it is still drawing, so every frame re-requests them and the map never fills.
     */
    public synchronized void ensureCapacity(int visibleTiles) {
        capacity = Math.max(DEFAULT_MAX_CACHED_TILES, Math.min(ABSOLUTE_MAX_CACHED_TILES, visibleTiles));
    }

    public synchronized void onVisionTile(IrisMessage.VisionTile tile) {
        IrisTileImage image;
        try {
            image = assembler.add(tile);
        } catch (ProtocolException malformed) {
            droppedMalformed++;
            return;
        }
        if (image == null) {
            return;
        }
        IrisTileKey key = new IrisTileKey(tile.tileX(), tile.tileZ(), tile.zoomLevel());
        cache.put(key, image);
        pending.remove(key);
        queued.remove(key);
    }

    public synchronized IrisTileImage get(IrisTileKey key) {
        return cache.get(key);
    }

    public synchronized long droppedMalformedCount() {
        return droppedMalformed;
    }

    public synchronized void resetRequestQueue() {
        queue.clear();
        queued.clear();
    }

    public synchronized void request(IrisTileKey key) {
        if (cache.containsKey(key)) {
            return;
        }
        long now = clock.getAsLong();
        Long lastRequest = pending.get(key);
        if (lastRequest != null && now - lastRequest < REQUEST_RETRY_MILLIS) {
            return;
        }
        if (queued.add(key)) {
            queue.addLast(key);
        }
    }

    public synchronized void pump() {
        long now = clock.getAsLong();
        if (now - windowStartMillis >= 1000L) {
            windowStartMillis = now;
            sentInWindow = 0;
            // An in-flight marker is only meaningful for one retry window. Sweeping it here keeps the map
            // bounded by tiles requested in the last few seconds instead of by every tile ever panned over.
            pending.values().removeIf((Long requestedAt) -> now - requestedAt >= REQUEST_RETRY_MILLIS);
        }
        while (sentInWindow < REQUESTS_PER_SECOND && !queue.isEmpty()) {
            IrisTileKey key = queue.pollFirst();
            queued.remove(key);
            if (cache.containsKey(key)) {
                continue;
            }
            Long lastRequest = pending.get(key);
            if (lastRequest != null && now - lastRequest < REQUEST_RETRY_MILLIS) {
                continue;
            }
            sink.send(IrisMessageCodec.encode(new IrisMessage.VisionTileRequest(key.tileX(), key.tileZ(), key.zoom())));
            pending.put(key, now);
            sentInWindow++;
        }
    }

    public synchronized void clear() {
        assembler.clear();
        cache.clear();
        pending.clear();
        queue.clear();
        queued.clear();
        capacity = DEFAULT_MAX_CACHED_TILES;
        sentInWindow = 0;
        windowStartMillis = 0L;
    }
}
