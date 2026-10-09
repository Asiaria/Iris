package art.arcane.iris.api.tree;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

public interface IrisTreeFellerService {
    boolean tryFell(BlockBreakEvent event, TreeFellerOptions options);

    boolean isManagedBreak(BlockBreakEvent event);

    boolean isTreeBlock(Block block);
}
