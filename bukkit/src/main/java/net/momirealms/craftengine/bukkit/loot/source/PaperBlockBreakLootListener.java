package net.momirealms.craftengine.bukkit.loot.source;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public final class PaperBlockBreakLootListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        Block block = event.getBlock();
        Key blockType = BlockStateUtils.getBlockOwner(block);
        List<LootSource> sources = LootSources.BLOCK_BREAK.getSources(blockType);
        if (sources.isEmpty()) return;
        Location location = block.getLocation();
        net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(location.getWorld());
        WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        BukkitExistingBlock bukkitExistingBlock = new BukkitExistingBlock(block);
        ContextHolder holder = ContextHolder.builder(
                DirectContextParameters.POSITION, position,
                DirectContextParameters.BLOCK, bukkitExistingBlock
        ).build();
        LootContext lootContext = new LootContext(world, null, 1.0f, holder);
        LootOutcome outcome = LootManager.eval(sources, lootContext);
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.getDrops().clear();
        }
        if (outcome.overwriteExperience()) {
            event.setExpToDrop(0);
        }
        for (Item item : outcome.items()) {
            world.dropItemNaturally(position, item);
        }
    }
}
