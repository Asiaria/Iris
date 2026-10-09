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

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes and decodes {@link IrisMessage} frames for the Iris plugin channel.
 * <p>
 * Stateless; safe from any thread. Each call allocates its own {@link IrisWireWriter} or
 * {@link IrisWireReader}, so no instance is shared. Encoding and decoding must stay symmetric - the field order
 * in each switch arm is the wire format.
 * <p>
 * Internal to Iris; not a published integration surface.
 */
public final class IrisMessageCodec {
    private IrisMessageCodec() {
    }

    /**
     * Encodes {@code message} into a frame: type id as a varint, then the record's fields in declaration order.
     * Never returns null.
     *
     * @throws IllegalStateException if the encoded form exceeds {@link IrisProtocol#MAX_FRAME_BYTES}, or if a
     *                               {@link IrisMessage.VisionMarkers} carries more than
     *                               {@link IrisProtocol#MAX_VISION_MARKERS} entries (the decoder rejects such a
     *                               frame, so producing one would only strand the payload)
     */
    public static byte[] encode(IrisMessage message) {
        IrisWireWriter writer = new IrisWireWriter();
        writer.writeVarInt(message.messageTypeId());
        switch (message) {
            case IrisMessage.ClientHello clientHello -> {
                writer.writeInt(clientHello.protocolVersion());
                writer.writeLong(clientHello.capabilities());
            }
            case IrisMessage.ServerHello serverHello -> {
                writer.writeInt(serverHello.protocolVersion());
                writer.writeLong(serverHello.capabilities());
                writer.writeString(serverHello.serverBrand());
                writer.writeBoolean(serverHello.irisActive());
            }
            case IrisMessage.PregenProgress pregenProgress -> {
                writer.writeLong(pregenProgress.jobId());
                writer.writeLong(pregenProgress.chunksDone());
                writer.writeLong(pregenProgress.chunksTotal());
                writer.writeDouble(pregenProgress.chunksPerSecond());
                writer.writeLong(pregenProgress.etaMillis());
                writer.writeInt(pregenProgress.state());
            }
            case IrisMessage.PregenEnd pregenEnd -> {
                writer.writeLong(pregenEnd.jobId());
                writer.writeBoolean(pregenEnd.completed());
            }
            case IrisMessage.DimensionStatus dimensionStatus -> {
                writer.writeString(dimensionStatus.dimensionKey());
                writer.writeString(dimensionStatus.packKey());
                writer.writeLong(dimensionStatus.seed());
                writer.writeInt(dimensionStatus.minY());
                writer.writeInt(dimensionStatus.maxY());
                writer.writeBoolean(dimensionStatus.irisWorld());
            }
            case IrisMessage.CursorInfoRequest cursorInfoRequest -> {
                writer.writeInt(cursorInfoRequest.blockX());
                writer.writeInt(cursorInfoRequest.blockZ());
            }
            case IrisMessage.CursorInfo cursorInfo -> {
                writer.writeInt(cursorInfo.blockX());
                writer.writeInt(cursorInfo.blockZ());
                writer.writeString(cursorInfo.biomeKey());
                writer.writeString(cursorInfo.regionKey());
                writer.writeString(cursorInfo.caveBiomeKey());
                writer.writeInt(cursorInfo.height());
                writer.writeString(cursorInfo.dimensionKey());
            }
            case IrisMessage.VisionTileRequest visionTileRequest -> {
                writer.writeInt(visionTileRequest.tileX());
                writer.writeInt(visionTileRequest.tileZ());
                writer.writeInt(visionTileRequest.zoomLevel());
            }
            case IrisMessage.VisionTile visionTile -> {
                writer.writeInt(visionTile.tileX());
                writer.writeInt(visionTile.tileZ());
                writer.writeInt(visionTile.zoomLevel());
                writer.writeInt(visionTile.sequence());
                writer.writeInt(visionTile.chunkIndex());
                writer.writeInt(visionTile.chunkCount());
                writer.writeBytes(visionTile.data());
            }
            case IrisMessage.VisionMarkers visionMarkers -> {
                writer.writeInt(visionMarkers.tileX());
                writer.writeInt(visionMarkers.tileZ());
                writer.writeInt(visionMarkers.zoomLevel());
                List<IrisMessage.VisionMarkers.Marker> markers = visionMarkers.markers();
                if (markers.size() > IrisProtocol.MAX_VISION_MARKERS) {
                    throw new IllegalStateException("marker count " + markers.size() + " exceeds " + IrisProtocol.MAX_VISION_MARKERS);
                }
                writer.writeVarInt(markers.size());
                for (IrisMessage.VisionMarkers.Marker marker : markers) {
                    writer.writeInt(marker.blockX());
                    writer.writeInt(marker.blockZ());
                    writer.writeInt(marker.kind());
                    writer.writeString(marker.label());
                }
            }
            case IrisMessage.PregenRegionDelta pregenRegionDelta -> {
                writer.writeLong(pregenRegionDelta.jobId());
                writer.writeInt(pregenRegionDelta.regionX());
                writer.writeInt(pregenRegionDelta.regionZ());
                writer.writeInt(pregenRegionDelta.state());
            }
            case IrisMessage.StudioHotload studioHotload -> {
                writer.writeString(studioHotload.packKey());
                writer.writeInt(studioHotload.changedFiles());
                writer.writeBoolean(studioHotload.failed());
                writer.writeString(studioHotload.message());
            }
            case IrisMessage.Toast toast -> {
                writer.writeInt(toast.kind());
                writer.writeString(toast.title());
                writer.writeString(toast.body());
            }
        }
        return writer.toByteArray();
    }

    /**
     * Decodes a frame produced by {@link #encode(IrisMessage)}.
     *
     * @return the decoded message, or null when the type id is unknown - which is how a newer peer's messages are
     *         skipped rather than treated as corruption. Callers must handle null.
     * @throws ProtocolException if {@code frame} is null, exceeds {@link IrisProtocol#MAX_FRAME_BYTES}, or is
     *                           truncated or otherwise malformed
     */
    public static IrisMessage decode(byte[] frame) throws ProtocolException {
        if (frame == null) {
            throw new ProtocolException("null frame");
        }
        if (frame.length > IrisProtocol.MAX_FRAME_BYTES) {
            throw new ProtocolException("frame exceeds " + IrisProtocol.MAX_FRAME_BYTES + " byte cap");
        }
        IrisWireReader reader = new IrisWireReader(frame);
        int messageTypeId = reader.readVarInt();
        return switch (messageTypeId) {
            case IrisProtocol.TYPE_CLIENT_HELLO -> new IrisMessage.ClientHello(reader.readInt(), reader.readLong());
            case IrisProtocol.TYPE_SERVER_HELLO -> new IrisMessage.ServerHello(reader.readInt(), reader.readLong(), reader.readString(), reader.readBoolean());
            case IrisProtocol.TYPE_PREGEN_PROGRESS -> new IrisMessage.PregenProgress(reader.readLong(), reader.readLong(), reader.readLong(), reader.readDouble(), reader.readLong(), reader.readInt());
            case IrisProtocol.TYPE_PREGEN_END -> new IrisMessage.PregenEnd(reader.readLong(), reader.readBoolean());
            case IrisProtocol.TYPE_DIMENSION_STATUS -> new IrisMessage.DimensionStatus(reader.readString(), reader.readString(), reader.readLong(), reader.readInt(), reader.readInt(), reader.readBoolean());
            case IrisProtocol.TYPE_CURSOR_INFO_REQUEST -> new IrisMessage.CursorInfoRequest(reader.readInt(), reader.readInt());
            case IrisProtocol.TYPE_CURSOR_INFO -> new IrisMessage.CursorInfo(reader.readInt(), reader.readInt(), reader.readString(), reader.readString(), reader.readString(), reader.readInt(), reader.readString());
            case IrisProtocol.TYPE_VISION_TILE_REQUEST -> new IrisMessage.VisionTileRequest(reader.readInt(), reader.readInt(), reader.readInt());
            case IrisProtocol.TYPE_VISION_TILE -> new IrisMessage.VisionTile(reader.readInt(), reader.readInt(), reader.readInt(), reader.readInt(), reader.readInt(), reader.readInt(), reader.readBytes());
            case IrisProtocol.TYPE_VISION_MARKERS -> decodeVisionMarkers(reader);
            case IrisProtocol.TYPE_PREGEN_REGION_DELTA -> new IrisMessage.PregenRegionDelta(reader.readLong(), reader.readInt(), reader.readInt(), reader.readInt());
            case IrisProtocol.TYPE_STUDIO_HOTLOAD -> new IrisMessage.StudioHotload(reader.readString(), reader.readInt(), reader.readBoolean(), reader.readString());
            case IrisProtocol.TYPE_TOAST -> new IrisMessage.Toast(reader.readInt(), reader.readString(), reader.readString());
            default -> null;
        };
    }

    private static IrisMessage.VisionMarkers decodeVisionMarkers(IrisWireReader reader) throws ProtocolException {
        int tileX = reader.readInt();
        int tileZ = reader.readInt();
        int zoomLevel = reader.readInt();
        int count = reader.readVarInt();
        if (count < 0) {
            throw new ProtocolException("negative marker count");
        }
        if (count > IrisProtocol.MAX_VISION_MARKERS) {
            throw new ProtocolException("marker count " + count + " exceeds " + IrisProtocol.MAX_VISION_MARKERS);
        }
        List<IrisMessage.VisionMarkers.Marker> markers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            markers.add(new IrisMessage.VisionMarkers.Marker(reader.readInt(), reader.readInt(), reader.readInt(), reader.readString()));
        }
        return new IrisMessage.VisionMarkers(tileX, tileZ, zoomLevel, markers);
    }
}
