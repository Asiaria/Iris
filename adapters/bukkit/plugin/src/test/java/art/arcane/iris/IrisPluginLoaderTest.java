package art.arcane.iris;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IrisPluginLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exposesOnlyRelocatedRuntimeLibrariesInStableOrder() throws Exception {
        Path root = temporaryFolder.newFolder("libraries").toPath();
        Path original = Files.createDirectories(root.resolve("gson/2.14.0"))
                .resolve("gson.jar");
        Path relocatedGson = Files.createDirectories(root.resolve("gson/2.14.0/relocated/Iris"))
                .resolve("gson.jar");
        Path relocatedCaffeine = Files.createDirectories(root.resolve("caffeine/3.2.4/relocated/Iris"))
                .resolve("caffeine.jar");
        Files.createFile(original);
        Files.createFile(relocatedGson);
        Files.createFile(relocatedCaffeine);

        List<Path> libraries = IrisPluginLoader.relocatedLibraries(root);

        assertEquals(List.of(relocatedCaffeine, relocatedGson), libraries);
    }
}
