/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2026 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package art.arcane.iris.spi.protocol;

import java.util.List;

/**
 * Every message the Iris plugin channel carries, as immutable records under one sealed interface.
 * <p>
 * Sealed so {@link IrisMessageCodec} can switch exhaustively - adding a permitted record forces the codec to
 * handle it. Records are immutable and safe to hand between threads, except that {@link VisionTile} holds its
 * payload array by reference and must not be mutated after construction.
 * <p>
 * Internal to Iris; not a published integration surface.
 */
public sealed interface IrisMessage {
    /**
     * The {@code IrisProtocol.TYPE_*} discriminator written as the first field of the encoded frame.
     */
    int messageTypeId();

    /**
     * Client to server, first frame: the client's protocol version and the capability bits it wants.
     */
    record ClientHello(int protocolVersion, long capabilities) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_CLIENT_HELLO;
        }
    }

    /**
     * Server to client, handshake reply: the version and capability bits actually granted, the server brand, and
     * whether any Iris world is live.
     */
    record ServerHello(int protocolVersion, long capabilities, String serverBrand, boolean irisActive) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_SERVER_HELLO;
        }
    }

    /**
     * Server to client: periodic pregeneration progress for one job.
     */
    record PregenProgress(long jobId, long chunksDone, long chunksTotal, double chunksPerSecond, long etaMillis, int state) implements IrisMessage {
        /** The job is generating. */
        public static final int STATE_RUNNING = 0;
        /** The job is paused and will resume. */
        public static final int STATE_PAUSED = 1;

        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_PREGEN_PROGRESS;
        }
    }

    /**
     * Server to client: a pregeneration job stopped. {@code completed} distinguishes finishing from being
     * cancelled or failing.
     */
    record PregenEnd(long jobId, boolean completed) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_PREGEN_END;
        }
    }

    /**
     * Server to client: which pack and height bounds back the dimension the player is in. {@code irisWorld} is
     * false for a vanilla dimension, in which case the other fields are placeholders.
     */
    record DimensionStatus(String dimensionKey, String packKey, long seed, int minY, int maxY, boolean irisWorld) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_DIMENSION_STATUS;
        }
    }

    /**
     * Client to server: what does the generator say about this column. Rate-limited by
     * {@link IrisProtocol#MAX_INBOUND_FRAMES_PER_SECOND}.
     */
    record CursorInfoRequest(int blockX, int blockZ) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_CURSOR_INFO_REQUEST;
        }
    }

    /**
     * Server to client: the answer to a {@link CursorInfoRequest}. {@code caveBiomeKey} is empty when no cave
     * biome applies at that column.
     */
    record CursorInfo(int blockX, int blockZ, String biomeKey, String regionKey, String caveBiomeKey, int height, String dimensionKey) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_CURSOR_INFO;
        }
    }

    /**
     * Client to server: render and send one vision map tile. Rate-limited by
     * {@link IrisProtocol#MAX_VISION_TILE_REQUESTS_PER_SECOND}.
     */
    record VisionTileRequest(int tileX, int tileZ, int zoomLevel) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_VISION_TILE_REQUEST;
        }
    }

    /**
     * Server to client: one chunk of a rendered tile. A tile larger than
     * {@link IrisProtocol#VISION_TILE_MAX_CHUNK_BYTES} arrives as {@code chunkCount} frames sharing a
     * {@code sequence}; the client reassembles by {@code chunkIndex} and discards a partial set when the
     * sequence changes. {@code data} is held by reference - do not mutate it after construction.
     */
    record VisionTile(int tileX, int tileZ, int zoomLevel, int sequence, int chunkIndex, int chunkCount, byte[] data) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_VISION_TILE;
        }
    }

    /**
     * Server to client: point-of-interest overlay for a tile, capped at {@link IrisProtocol#MAX_VISION_MARKERS}
     * entries.
     */
    record VisionMarkers(int tileX, int tileZ, int zoomLevel, List<Marker> markers) implements IrisMessage {
        /**
         * One overlay marker at a block position. {@code kind} is a client-side icon selector.
         */
        public record Marker(int blockX, int blockZ, int kind, String label) {
        }

        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_VISION_MARKERS;
        }
    }

    /**
     * Server to client: one region of a pregeneration job changed state. Sent as a delta so the client can paint
     * a live progress grid without a full snapshot per tick.
     */
    record PregenRegionDelta(long jobId, int regionX, int regionZ, int state) implements IrisMessage {
        /** Queued, not started. */
        public static final int STATE_PENDING = 0;
        /** Currently generating. */
        public static final int STATE_GENERATING = 1;
        /** Finished and saved. */
        public static final int STATE_DONE = 2;

        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_PREGEN_REGION_DELTA;
        }
    }

    /**
     * Server to client: a studio pack reload happened. {@code failed} marks a reload that did not apply, with
     * {@code message} carrying the reason.
     */
    record StudioHotload(String packKey, int changedFiles, boolean failed, String message) implements IrisMessage {
        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_STUDIO_HOTLOAD;
        }
    }

    /**
     * Server to client: show a transient notification.
     */
    record Toast(int kind, String title, String body) implements IrisMessage {
        /** Neutral notice. */
        public static final int KIND_INFO = 0;
        /** Operation succeeded. */
        public static final int KIND_SUCCESS = 1;
        /** Something needs attention. */
        public static final int KIND_WARNING = 2;
        /** Operation failed. */
        public static final int KIND_ERROR = 3;

        @Override
        public int messageTypeId() {
            return IrisProtocol.TYPE_TOAST;
        }
    }
}
