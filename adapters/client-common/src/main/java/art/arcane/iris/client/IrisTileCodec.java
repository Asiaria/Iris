package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.ProtocolException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class IrisTileCodec {
    public static final int MODE_RAW_RGB = 0;
    public static final int MODE_PALETTE = 1;
    private static final int MAX_DIMENSION = 512;
    private static final int OPAQUE = 0xFF000000;
    static final int MAX_DECODED_BYTES = 9 + 4 + 3 * MAX_DIMENSION * MAX_DIMENSION;

    private IrisTileCodec() {
    }

    /**
     * Decodes an assembled tile blob.
     *
     * @return null when {@code deflatedBlob} is null or empty - nothing to decode is not an error
     * @throws ProtocolException when the blob is present but malformed: a corrupt or stalled deflate stream, an
     *                           oversized payload, bad dimensions, an unknown pixel mode, a bad palette or a
     *                           truncated pixel run. Callers drop the tile and count it; the wire is untrusted.
     */
    public static IrisTileImage decode(byte[] deflatedBlob) throws ProtocolException {
        if (deflatedBlob == null || deflatedBlob.length == 0) {
            return null;
        }
        byte[] raw = inflate(deflatedBlob);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int width = in.readInt();
            int height = in.readInt();
            if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                throw new ProtocolException("vision tile dimensions " + width + "x" + height + " out of range");
            }
            int mode = in.readUnsignedByte();
            int[] argb = new int[width * height];
            return switch (mode) {
                case MODE_PALETTE -> decodePalette(in, width, height, argb);
                case MODE_RAW_RGB -> decodeRaw(in, width, height, argb);
                default -> throw new ProtocolException("unknown vision tile mode " + mode);
            };
        } catch (EOFException truncated) {
            throw new ProtocolException("vision tile blob truncated");
        } catch (IOException failure) {
            throw new ProtocolException("vision tile blob unreadable: " + failure.getMessage());
        }
    }

    private static IrisTileImage decodePalette(DataInputStream in, int width, int height, int[] argb) throws IOException, ProtocolException {
        int paletteSize = in.readInt();
        if (paletteSize <= 0 || paletteSize > 256) {
            throw new ProtocolException("vision tile palette size " + paletteSize + " out of range");
        }
        int[] palette = new int[paletteSize];
        for (int index = 0; index < paletteSize; index++) {
            int red = in.readUnsignedByte();
            int green = in.readUnsignedByte();
            int blue = in.readUnsignedByte();
            palette[index] = OPAQUE | red << 16 | green << 8 | blue;
        }
        for (int pixel = 0; pixel < argb.length; pixel++) {
            int paletteIndex = in.readUnsignedByte();
            if (paletteIndex >= paletteSize) {
                throw new ProtocolException("vision tile palette index " + paletteIndex + " beyond size " + paletteSize);
            }
            argb[pixel] = palette[paletteIndex];
        }
        return new IrisTileImage(width, height, argb);
    }

    private static IrisTileImage decodeRaw(DataInputStream in, int width, int height, int[] argb) throws IOException {
        for (int pixel = 0; pixel < argb.length; pixel++) {
            int red = in.readUnsignedByte();
            int green = in.readUnsignedByte();
            int blue = in.readUnsignedByte();
            argb[pixel] = OPAQUE | red << 16 | green << 8 | blue;
        }
        return new IrisTileImage(width, height, argb);
    }

    private static byte[] inflate(byte[] input) throws ProtocolException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, input.length * 2));
        byte[] buffer = new byte[8192];
        try {
            while (!inflater.finished()) {
                int produced = inflater.inflate(buffer);
                if (produced == 0) {
                    if (inflater.finished()) {
                        break;
                    }
                    // needsInput here means the stream ended mid-member; needsDictionary means it wants a
                    // preset dictionary the encoder never uses. Either way there is no more progress to make,
                    // and looping on a zero-progress inflater spins a render thread forever.
                    throw new ProtocolException(inflater.needsDictionary()
                            ? "vision tile stream wants a preset dictionary"
                            : "vision tile stream ended before the deflate member finished");
                }
                if (out.size() + produced > MAX_DECODED_BYTES) {
                    throw new ProtocolException("vision tile inflates beyond " + MAX_DECODED_BYTES + " bytes");
                }
                out.write(buffer, 0, produced);
            }
        } catch (DataFormatException malformed) {
            throw new ProtocolException("vision tile stream malformed: " + malformed.getMessage());
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }
}
