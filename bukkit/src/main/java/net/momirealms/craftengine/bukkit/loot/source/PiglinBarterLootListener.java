package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;

import java.util.List;

public final class PiglinBarterLootListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBarter(PiglinBarterEvent event) {
        List<LootSource> sources = LootSources.PIGLIN_BARTER.getSources(null);
        if (sources.isEmpty()) return;
        Location location = event.getEntity().getLocation();
        World world = BukkitAdaptor.adapt(location.getWorld());
        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(location))
                .withParameter(DirectContextParameters.THIS_ENTITY, BukkitAdaptor.adapt(event.getEntity()))
                .withParameter(DirectContextParameters.ITEM, BukkitItemManager.instance().wrap(event.getInput()))
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, null, 1f, holder));
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.getOutcome().clear();
        }
        event.getOutcome().addAll(ItemStackUtils.getBukkitStacks(outcome.items()));
    }
}
