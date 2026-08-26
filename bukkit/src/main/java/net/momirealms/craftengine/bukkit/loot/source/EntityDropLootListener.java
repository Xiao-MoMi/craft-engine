package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.EntityManager;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;

import java.util.List;

public final class EntityDropLootListener implements Listener {
    private final EntityManager entityManager;

    public EntityDropLootListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (event.getEntity() instanceof Player) return;
        BukkitEntity bukkitEntity = BukkitAdaptor.adapt(event.getEntity());
        List<LootSource> sources = LootSources.ENTITY_DROP.getSources(this.entityManager.getEntityId(bukkitEntity));
        if (sources.isEmpty()) return;
        Location location = event.getItemDrop().getLocation();
        World world = BukkitAdaptor.adapt(location.getWorld());
        WorldPosition worldPosition = LocationUtils.toWorldPosition(location);
        ContextHolder holder = ContextHolder.builder(
                DirectContextParameters.WORLD, world,
                DirectContextParameters.POSITION, worldPosition,
                DirectContextParameters.ENTITY, bukkitEntity
        ).build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, null, 1f, holder));
        if (!outcome.matched()) return;
        List<Item> items = outcome.items();
        if (outcome.overwriteItems() && !items.isEmpty()) {
            event.getItemDrop().setItemStack(ItemStackUtils.getBukkitStack(items.removeFirst()));
        }
        for (Item extra : items) {
            world.dropItemNaturally(worldPosition, extra);
        }
    }
}
