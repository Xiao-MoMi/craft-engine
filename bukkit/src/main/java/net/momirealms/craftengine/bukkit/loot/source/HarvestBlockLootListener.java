package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public final class HarvestBlockLootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Key blockType = BukkitAdaptor.adapt(event.getHarvestedBlock()).id();
        List<LootSource> sources = LootSources.HARVEST.getSources(blockType);
        if (sources.isEmpty()) return;
        Player player = event.getPlayer();
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        Location location = LocationUtils.toBlockCenterLocation(event.getHarvestedBlock().getLocation());
        World world = BukkitAdaptor.adapt(location.getWorld());
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.ENTITY, serverPlayer)
                .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(location))
                .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, serverPlayer.getItemInHand(hand))
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, (float) serverPlayer.luck(), holder));
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.getItemsHarvested().clear();
        }
        event.getItemsHarvested().addAll(ItemStackUtils.getBukkitStacks(outcome.items()));
    }
}
