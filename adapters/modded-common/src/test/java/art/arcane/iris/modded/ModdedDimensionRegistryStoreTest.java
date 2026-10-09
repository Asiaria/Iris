package art.arcane.iris.modded;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ModdedDimensionRegistryStoreTest {
    @Test
    public void registryRoundTripsPersistentDimensions() throws IOException {
        Path root = Files.createTempDirectory("iris-dimension-registry");
        Path file = root.resolve("iris-dimensions.json");
        try {
            List<ModdedDimensionRegistryStore.PersistentDimension> expected = List.of(
                    new ModdedDimensionRegistryStore.PersistentDimension(
                            "iris:first", "overworld", "overworld", 42L),
                    new ModdedDimensionRegistryStore.PersistentDimension(
                            "iris:second", "other", "surface", -9L));

            ModdedDimensionRegistryStore.write(file, expected);

            assertEquals(expected, ModdedDimensionRegistryStore.load(file));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(root);
        }
    }

    @Test
    public void malformedEntryDoesNotDiscardHealthyEntries() throws IOException {
        Path root = Files.createTempDirectory("iris-dimension-registry-partial");
        Path file = root.resolve("iris-dimensions.json");
        try {
            Files.writeString(file, """
                    {
                      "dimensions": [
                        {"id":"iris:good","pack":"overworld","dimension":"overworld","seed":7},
                        {"id":"iris:broken","pack":"overworld"}
                      ]
                    }
                    """, StandardCharsets.UTF_8);

            assertEquals(List.of(new ModdedDimensionRegistryStore.PersistentDimension(
                    "iris:good", "overworld", "overworld", 7L)),
                    ModdedDimensionRegistryStore.load(file));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(root);
        }
    }

    @Test
    public void truncatedRegistryNeverBecomesAnEmptySuccessfulLoad() throws IOException {
        Path root = Files.createTempDirectory("iris-dimension-registry-truncated");
        Path file = root.resolve("iris-dimensions.json");
        try {
            Files.writeString(file, "{\"dimensions\":[", StandardCharsets.UTF_8);

            assertThrows(IllegalStateException.class,
                    () -> ModdedDimensionRegistryStore.load(file));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(root);
        }
    }

    @Test
    public void missingDimensionsArrayNeverBecomesAnEmptySuccessfulLoad() throws IOException {
        Path root = Files.createTempDirectory("iris-dimension-registry-missing-root");
        Path file = root.resolve("iris-dimensions.json");
        try {
            Files.writeString(file, "{}", StandardCharsets.UTF_8);

            assertThrows(IllegalStateException.class,
                    () -> ModdedDimensionRegistryStore.load(file));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(root);
        }
    }

    @Test
    public void startupLoadQuarantinesACorruptRegistryInsteadOfFailingBoot() throws IOException {
        Path root = Files.createTempDirectory("iris-dimension-registry-corrupt-boot");
        Path file = root.resolve("iris-dimensions.json");
        try {
            Files.writeString(file, "{\"dimensions\":[{\"id\":\"iris:lost\",", StandardCharsets.UTF_8);

            assertEquals(List.of(), ModdedDimensionRegistryStore.loadForStartup(file));
            assertFalse(Files.exists(file));

            try (Stream<Path> entries = Files.list(root)) {
                assertTrue(entries.anyMatch((Path entry) ->
                        entry.getFileName().toString().startsWith("iris-dimensions.json.broken-")));
            }
        } finally {
            deleteTree(root);
        }
    }

    @Test
    public void startupLoadReturnsHealthyEntriesUntouched() throws IOException {
        Path root = Files.createTempDirectory("iris-dimension-registry-healthy-boot");
        Path file = root.resolve("iris-dimensions.json");
        try {
            List<ModdedDimensionRegistryStore.PersistentDimension> expected = List.of(
                    new ModdedDimensionRegistryStore.PersistentDimension(
                            "iris:first", "overworld", "overworld", 42L));
            ModdedDimensionRegistryStore.write(file, expected);

            assertEquals(expected, ModdedDimensionRegistryStore.loadForStartup(file));
            assertTrue(Files.exists(file));
        } finally {
            deleteTree(root);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        List<Path> entries;
        try (Stream<Path> walk = Files.walk(root)) {
            entries = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path entry : entries) {
            Files.deleteIfExists(entry);
        }
    }
}
