package net.momirealms.craftengine.bukkit.loot.source;

import io.papermc.paper.event.block.PlayerShearBlockEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
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
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public final class ShearBlockLootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShearBlock(PlayerShearBlockEvent event) {
        Key blockType = BlockStateUtils.getBlockOwner(event.getBlock());
        List<LootSource> sources = LootSources.BLOCK_SHEAR.getSources(blockType);
        if (sources.isEmpty()) return;
        Player player = event.getPlayer();
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        Location location = LocationUtils.toBlockCenterLocation(event.getBlock().getLocation());
        World world = BukkitAdaptor.adapt(location.getWorld());
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(location))
                .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, serverPlayer.getItemInHand(hand))
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, (float) serverPlayer.luck(), holder));
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.getDrops().clear();
        }
        event.getDrops().addAll(ItemStackUtils.getBukkitStacks(outcome.items()));
    }
}
