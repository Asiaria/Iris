package art.arcane.iris.probe;

import art.arcane.volmlib.util.collection.KMap;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StubTileDataTest {
    @Test
    public void modernTilePayloadRoundTripsWithoutBukkit() throws Exception {
        KMap<String, Object> properties = new KMap<>();
        properties.put("LootTable", "minecraft:chests/ancient_city");
        StubTileData original = StubTileData.fromProperties(
                StubPlatform.blockStateForTest("minecraft:chest[facing=north]"), properties);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            original.toBinary(output);
        }

        StubTileData decoded;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            decoded = StubTileData.read(input);
        }

        assertEquals("minecraft:chest", decoded.blockKey());
        assertEquals("minecraft:chests/ancient_city", decoded.getProperties().get("LootTable"));
    }

    @Test
    public void modernIntegralPropertiesReencodeByteIdentical() throws Exception {
        KMap<String, Object> properties = new KMap<>();
        properties.put("LootTable", "minecraft:chests/ancient_city");
        properties.put("LootTableSeed", -7205759403792793599L);
        StubTileData original = StubTileData.fromProperties(
                StubPlatform.blockStateForTest("minecraft:chest[facing=north]"), properties);
        byte[] encoded = encode(original::toBinary);

        StubTileData decoded;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(encoded))) {
            decoded = StubTileData.read(input);
        }

        assertArrayEquals(encoded, encode(decoded::toBinary));
        assertEquals(-7205759403792793599L, decoded.getProperties().get("LootTableSeed"));
    }

    @Test
    public void legacyLootablePayloadPreservesItsCompleteFrame() throws Exception {
        byte[] payload = encode(output -> {
            output.writeShort(3);
            output.writeUTF("minecraft:chest");
            output.writeUTF("minecraft:chests/ancient_city");
            output.writeLong(998877L);
        });

        StubTileData decoded = decode(payload);

        assertEquals("minecraft:chest", decoded.blockKey());
        assertEquals("minecraft:chests/ancient_city", decoded.getProperties().get("LootTable"));
        assertArrayEquals(payload, encode(decoded::toBinary));
    }

    @Test
    public void legacySpawnerAndBannerFramesRemainUnambiguous() throws Exception {
        byte[] ordinalSpawner = encode(output -> {
            output.writeShort(1);
            output.writeShort(42);
        });
        byte[] keyedBanner = encode(output -> {
            output.writeShort(2);
            output.writeByte(11);
            output.writeByte(1);
            output.writeByte(3);
            output.writeUTF("minecraft:base");
        });

        assertArrayEquals(ordinalSpawner, encode(decode(ordinalSpawner)::toBinary));
        assertArrayEquals(keyedBanner, encode(decode(keyedBanner)::toBinary));
    }

    private static StubTileData decode(byte[] payload) throws Exception {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            return StubTileData.read(input);
        }
    }

    private static byte[] encode(BinaryWriter writer) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writer.write(output);
        }
        return bytes.toByteArray();
    }

    @FunctionalInterface
    private interface BinaryWriter {
        void write(DataOutputStream output) throws Exception;
    }
}
