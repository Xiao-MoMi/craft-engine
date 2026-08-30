package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.BrushableBlockEntityProxy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public final class ArchaeologyLootListener implements Listener {

    private static boolean isBrushable(Material type) {
        return type == Material.SUSPICIOUS_SAND || type == Material.SUSPICIOUS_GRAVEL;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBrush(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Item tool = ItemStackUtils.wrap(event.getItem());
        if (tool.isEmpty() || !tool.vanillaId().equals(ItemKeys.BRUSH)) return;

        Object blockPos = LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ());
        Object blockEntity = LevelProxy.INSTANCE.getBlockEntity(CraftWorldProxy.INSTANCE.getWorld(block.getWorld()), blockPos);
        if (!BrushableBlockEntityProxy.CLASS.isInstance(blockEntity)) return;

        Object lootTableKey = BrushableBlockEntityProxy.INSTANCE.getLootTable(blockEntity);
        if (lootTableKey == null) return;
        Object identifier = ResourceKeyProxy.INSTANCE.getIdentifier(lootTableKey);
        Key tableKey = KeyUtils.identifierToKey(identifier);
        List<LootSource> sources = LootSources.ARCHAEOLOGY.getSources(tableKey);
        if (sources.isEmpty()) return;

        Player player = event.getPlayer();
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        World world = BukkitAdaptor.adapt(block.getWorld());
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;

        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.ENTITY, serverPlayer)
                .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()))
                .withParameter(DirectContextParameters.ITEM_IN_HAND, serverPlayer.getItemInHand(hand))
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, (float) serverPlayer.luck(), holder));
        if (!outcome.matched()) return;
        Item first = outcome.items().isEmpty() ? null : outcome.items().getFirst();
        if (first != null) {
            BrushableBlockEntityProxy.INSTANCE.setLootTable(blockEntity, null);
            BrushableBlockEntityProxy.INSTANCE.setItem(blockEntity, first.minecraftItem());
        }
    }
}
