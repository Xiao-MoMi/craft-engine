package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.EntityManager;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class EntityShearLootListener implements Listener {
    private final EntityManager entityManager;

    public EntityShearLootListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShearEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        BukkitEntity bukkitEntity = BukkitAdaptor.adapt(event.getEntity());
        List<LootSource> sources = LootSources.ENTITY_SHEAR.getSources(this.entityManager.getEntityId(bukkitEntity));
        if (sources.isEmpty()) return;
        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        World world = BukkitAdaptor.adapt(event.getEntity().getWorld());
        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                .withParameter(DirectContextParameters.ENTITY, serverPlayer)
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(event.getEntity().getLocation()))
                .withParameter(DirectContextParameters.THIS_ENTITY, bukkitEntity)
                .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, serverPlayer.getItemInHand(hand))
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, (float) serverPlayer.luck(), holder));
        if (!outcome.matched()) return;
        List<ItemStack> drops = outcome.overwriteItems() ? new ArrayList<>(4) : new ArrayList<>(event.getDrops());
        drops.addAll(ItemStackUtils.getBukkitStacks(outcome.items()));
        event.setDrops(drops);
    }
}
