package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.Player;
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
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ContainerLootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        Key tableKey = KeyUtils.namespacedKeyToKey(event.getLootTable().getKey());
        List<LootSource> sources = LootSources.CONTAINER.getSources(tableKey);
        if (sources.isEmpty()) return;
        Location location = event.getLootContext().getLocation();
        World world = BukkitAdaptor.adapt(location.getWorld());
        ContextHolder.Builder builder = ContextHolder.builder()
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(location));
        Entity entity = event.getEntity();
        Player player = null;
        if (entity != null) {
            BukkitEntity adaptedEntity = BukkitAdaptor.adapt(event.getEntity());
            builder.withParameter(DirectContextParameters.ENTITY, adaptedEntity);
            if (adaptedEntity instanceof Player cePlayer) {
                builder.withParameter(DirectContextParameters.ENTITY, cePlayer)
                        .withParameter(DirectContextParameters.PLAYER, cePlayer);
                player = cePlayer;
            }
        }
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, player, event.getLootContext().getLuck(), builder.build()));
        if (!outcome.matched()) return;
        List<ItemStack> loot = event.getLoot();
        if (outcome.overwriteItems()) {
            loot.clear();
        }
        loot.addAll(ItemStackUtils.getBukkitStacks(outcome.items()));
    }
}
