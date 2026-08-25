package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class VaultLootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispenseLoot(BlockDispenseLootEvent event) {
        Key tableKey = KeyUtils.namespacedKeyToKey(event.getLootTable().getKey());
        List<LootSource> sources = LootSources.VAULT.getSources(tableKey);
        if (sources.isEmpty()) return;
        Block block = event.getBlock();
        World world = BukkitAdaptor.adapt(block.getWorld());
        ContextHolder.Builder builder = ContextHolder.builder()
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(block.getLocation()));
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = null;
        if (player != null) {
            serverPlayer = BukkitAdaptor.adapt(player);
            builder.withParameter(DirectContextParameters.PLAYER, serverPlayer);
            builder.withParameter(DirectContextParameters.ENTITY, serverPlayer);
        }
        float luck = serverPlayer != null ? (float) serverPlayer.luck() : 1f;
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, luck, builder.build()));
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.setDispensedLoot(ItemStackUtils.getBukkitStacks(outcome.items()));
        } else {
            List<ItemStack> dispensed = new ArrayList<>(event.getDispensedLoot());
            dispensed.addAll(ItemStackUtils.getBukkitStacks(outcome.items()));
            event.setDispensedLoot(dispensed);
        }
    }
}
