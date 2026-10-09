package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;
import art.arcane.iris.spi.protocol.IrisProtocol;
import art.arcane.iris.spi.protocol.ProtocolException;

import java.util.LinkedHashMap;
import java.util.Map;

final class IrisTileAssembler {
    /** More chunks than the largest legal tile can occupy means the header is lying. */
    static final int MAX_CHUNK_COUNT =
            (IrisTileCodec.MAX_DECODED_BYTES + IrisProtocol.VISION_TILE_MAX_CHUNK_BYTES - 1)
                    / IrisProtocol.VISION_TILE_MAX_CHUNK_BYTES + 1;
    /** Half-assembled tiles retained at once; the oldest is dropped past this. */
    static final int MAX_PENDING_TILES = 64;

    private final Map<IrisTileKey, Partial> partials;

    IrisTileAssembler() {
        this.partials = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IrisTileKey, Partial> eldest) {
                return size() > MAX_PENDING_TILES;
            }
        };
    }

    /**
     * @return the finished image once the last chunk of a set lands, or null while the set is incomplete or the
     *         frame is structurally unusable (impossible index or count, missing payload, a chunk from a set
     *         older than the one being assembled)
     * @throws ProtocolException when a complete set decodes to a malformed blob
     */
    IrisTileImage add(IrisMessage.VisionTile tile) throws ProtocolException {
        int chunkCount = tile.chunkCount();
        int chunkIndex = tile.chunkIndex();
        if (chunkCount <= 0 || chunkCount > MAX_CHUNK_COUNT || chunkIndex < 0 || chunkIndex >= chunkCount || tile.data() == null) {
            return null;
        }
        IrisTileKey key = new IrisTileKey(tile.tileX(), tile.tileZ(), tile.zoomLevel());
        Partial partial = partials.get(key);
        if (partial != null && tile.sequence() < partial.sequence()) {
            return null;
        }
        if (partial == null || tile.sequence() > partial.sequence() || partial.chunkCount() != chunkCount) {
            partial = new Partial(tile.sequence(), chunkCount);
            partials.put(key, partial);
        }
        if (!partial.accept(chunkIndex, tile.data())) {
            return null;
        }
        partials.remove(key);
        return IrisTileCodec.decode(partial.concat());
    }

    void clear() {
        partials.clear();
    }

    private static final class Partial {
        private final int sequence;
        private final int chunkCount;
        private final byte[][] chunks;
        private int received;

        Partial(int sequence, int chunkCount) {
            this.sequence = sequence;
            this.chunkCount = chunkCount;
            this.chunks = new byte[chunkCount][];
            this.received = 0;
        }

        int sequence() {
            return sequence;
        }

        int chunkCount() {
            return chunkCount;
        }

        boolean accept(int chunkIndex, byte[] data) {
            if (chunks[chunkIndex] == null) {
                chunks[chunkIndex] = data;
                received++;
            }
            return received >= chunkCount;
        }

        byte[] concat() {
            int total = 0;
            for (byte[] chunk : chunks) {
                total += chunk.length;
            }
            byte[] blob = new byte[total];
            int position = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, blob, position, chunk.length);
                position += chunk.length;
            }
            return blob;
        }
    }
}
