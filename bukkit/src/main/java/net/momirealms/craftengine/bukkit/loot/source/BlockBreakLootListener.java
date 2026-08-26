package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ExplosionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public final class BlockBreakLootListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        Block block = event.getBlock();
        Key blockType = BlockStateUtils.getBlockOwner(block);
        List<LootSource> sources = LootSources.BLOCK_BREAK.getSources(blockType);
        if (sources.isEmpty()) return;
        if (!event.isDropItems()) return;
        Location location = block.getLocation();
        World world = BukkitAdaptor.adapt(location.getWorld());
        WorldPosition position = new WorldPosition(world, location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        BukkitExistingBlock bukkitExistingBlock = new BukkitExistingBlock(block);
        Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        LootContext lootContext = new LootContext(world, serverPlayer, (float) serverPlayer.luck(), ContextHolder.builder(
                DirectContextParameters.PLAYER, serverPlayer,
                DirectContextParameters.ITEM_IN_HAND, itemInHand,
                DirectContextParameters.BLOCK, bukkitExistingBlock,
                DirectContextParameters.POSITION, position
        ).build());
        LootOutcome outcome = LootManager.eval(sources, lootContext);
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.setDropItems(false);
        }
        if (outcome.overwriteExperience()) {
            event.setExpToDrop(0);
        }
        for (Item item : outcome.items()) {
            world.dropItemNaturally(position, item);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        float radius;
        if (VersionHelper.isOrAbove1_21) {
            if (!ExplosionUtils.isDroppingItems(event)) return;
            radius = ExplosionUtils.getRadius(event.getYield(), event.getExplosionResult());
        } else {
            radius = 1 / event.getYield();
        }
        handleExplosion(BukkitAdaptor.adapt(event.getBlock().getWorld()), event.blockList(), radius, null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        float radius;
        if (VersionHelper.isOrAbove1_21) {
            if (!ExplosionUtils.isDroppingItems(event)) return;
            radius = ExplosionUtils.getRadius(event.getYield(), event.getExplosionResult());
        } else {
            radius = 1 / event.getYield();
        }
        Entity entity = event.getEntity();
        handleExplosion(BukkitAdaptor.adapt(entity.getWorld()), event.blockList(), radius, entity);
    }

    private void handleExplosion(World world, List<Block> blocks, float radius, @Nullable Entity entity) {
        net.momirealms.craftengine.core.entity.Entity ceEntity = entity == null ? null : BukkitAdaptor.adapt(entity);
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Key blockType = BlockStateUtils.getBlockOwner(block);
            List<LootSource> sources = LootSources.BLOCK_BREAK.getSources(blockType);
            if (sources.isEmpty()) continue;
            WorldPosition position = new WorldPosition(world, block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
            ContextHolder holder = ContextHolder.builder(
                            DirectContextParameters.BLOCK, new BukkitExistingBlock(block),
                            DirectContextParameters.POSITION, position,
                            DirectContextParameters.EXPLOSION_RADIUS, radius
                    )
                    .withOptionalParameter(DirectContextParameters.ENTITY, ceEntity)
                    .build();
            LootContext lootContext = new LootContext(world, null, 1f, holder);
            LootOutcome outcome = LootManager.eval(sources, lootContext);
            if (!outcome.matched()) return;
            if (outcome.overwriteItems()) {
                iterator.remove();
                LevelProxy.INSTANCE.removeBlock(world.minecraftWorld(), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()), false);
            }
            for (Item item : outcome.items()) {
                world.dropItemNaturally(position, item);
            }
        }
    }
}
