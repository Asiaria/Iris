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

/**
 * Wire constants for the Iris client/server plugin channel: version, channel name, size and rate caps,
 * capability bits and message type ids.
 * <p>
 * Both ends of the channel compile against this class, so every value here is part of the wire contract. Adding
 * a message type or capability bit is compatible; changing an existing value is not and requires bumping
 * {@link #PROTOCOL_VERSION}. Constants only - no state, safe from any thread.
 * <p>
 * Internal to Iris; not a published integration surface.
 */
public final class IrisProtocol {
    /** Wire version exchanged in the hello handshake. Bumped on any incompatible change. */
    public static final int PROTOCOL_VERSION = 1;
    /** Plugin channel both ends register. */
    public static final String CHANNEL = "irisworldgen:main";
    /** Hard cap on a single encoded frame. {@link IrisWireWriter} refuses to exceed it; the decoder rejects larger. */
    public static final int MAX_FRAME_BYTES = 24576;
    /** Inbound frames accepted per client per second before the server sheds. */
    public static final int MAX_INBOUND_FRAMES_PER_SECOND = 32;
    /** Vision tile requests accepted per client per second, tighter than the general frame budget because each one costs a render. */
    public static final int MAX_VISION_TILE_REQUESTS_PER_SECOND = 8;
    /**
     * Cursor lookups accepted per client per second. Its own budget rather than a slice of
     * {@link #MAX_INBOUND_FRAMES_PER_SECOND}: each lookup drives three engine column queries, so a client that
     * spent its whole frame budget on cursors would cost 32 column resolves per second per player.
     */
    public static final int MAX_CURSOR_INFO_REQUESTS_PER_SECOND = 4;
    /**
     * Largest absolute block coordinate a client may ask the generator about - the vanilla world border limit.
     * Coordinates outside it are rejected rather than clamped so a spoofed frame is counted, not served.
     */
    public static final int MAX_QUERY_BLOCK_COORDINATE = 29_999_999;
    /** Fixed header size of a vision tile frame, subtracted when splitting a tile into chunks. */
    public static final int VISION_TILE_HEADER_BYTES = 25;
    /** Largest payload carried by one vision tile chunk. */
    public static final int VISION_TILE_MAX_CHUNK_BYTES = 24000;
    /** Cap on markers in one {@link IrisMessage.VisionMarkers} frame. */
    public static final int MAX_VISION_MARKERS = 256;
    /** Capability bit: pregeneration progress streaming. */
    public static final long CAPABILITY_PREGEN = 1L << 0;
    /** Capability bit: vision map tiles and markers. */
    public static final long CAPABILITY_VISION = 1L << 1;
    /** Capability bit: cursor coordinate lookups. */
    public static final long CAPABILITY_CURSOR = 1L << 2;
    /** Capability bit: studio hotload notifications. */
    public static final long CAPABILITY_STUDIO = 1L << 3;
    public static final int TYPE_CLIENT_HELLO = 1;
    public static final int TYPE_SERVER_HELLO = 2;
    public static final int TYPE_PREGEN_PROGRESS = 3;
    public static final int TYPE_PREGEN_END = 4;
    public static final int TYPE_DIMENSION_STATUS = 5;
    public static final int TYPE_CURSOR_INFO_REQUEST = 6;
    public static final int TYPE_CURSOR_INFO = 7;
    public static final int TYPE_VISION_TILE_REQUEST = 8;
    public static final int TYPE_VISION_TILE = 9;
    public static final int TYPE_VISION_MARKERS = 10;
    public static final int TYPE_PREGEN_REGION_DELTA = 11;
    public static final int TYPE_STUDIO_HOTLOAD = 12;
    public static final int TYPE_TOAST = 13;

    private IrisProtocol() {
    }
}
