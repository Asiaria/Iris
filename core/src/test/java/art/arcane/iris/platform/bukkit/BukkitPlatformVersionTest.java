package art.arcane.iris.platform.bukkit;

import org.bukkit.Server;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class BukkitPlatformVersionTest {
    private interface PaperLikeServer extends Server {
        String getMinecraftVersion();
    }

    @Test
    public void reportsCanonicalMinecraftVersionInsteadOfBukkitBuildVersion() {
        PaperLikeServer server = mock(PaperLikeServer.class);
        doReturn("26.2").when(server).getMinecraftVersion();
        doReturn("26.2.build.33-alpha").when(server).getBukkitVersion();

        assertEquals("26.2", BukkitPlatform.minecraftVersion(server));
    }
}
