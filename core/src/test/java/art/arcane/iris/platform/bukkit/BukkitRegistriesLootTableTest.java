package art.arcane.iris.platform.bukkit;

import org.bukkit.loot.LootTables;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BukkitRegistriesLootTableTest {
    @Test
    public void lootTableKeysDoNotRequireLiveServerRegistryAccess() {
        List<String> expected = new ArrayList<>();
        for (LootTables table : LootTables.values()) {
            expected.add(table.getKey().toString());
        }

        assertEquals(expected, new BukkitRegistries().lootTableKeys());
    }
}
