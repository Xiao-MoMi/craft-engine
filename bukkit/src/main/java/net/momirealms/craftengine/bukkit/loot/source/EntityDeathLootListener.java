package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.loot.BukkitLootContextParameters;
import net.momirealms.craftengine.core.entity.EntityManager;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public final class EntityDeathLootListener implements Listener {
    private final EntityManager entityManager;

    public EntityDeathLootListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        BukkitEntity bukkitEntity = BukkitAdaptor.adapt(entity);
        List<LootSource> sources = LootSources.ENTITY_DEATH.getSources(this.entityManager.getEntityId(bukkitEntity));
        if (sources.isEmpty()) return;
        Location location = entity.getLocation();
        net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(entity.getWorld());
        WorldPosition position = new WorldPosition(world, location.getX(), location.getY(), location.getZ());
        ContextHolder.Builder builder = ContextHolder.builder()
                .withParameter(DirectContextParameters.THIS_ENTITY, bukkitEntity)
                .withParameter(DirectContextParameters.POSITION, position)
                // 实体死亡必有致死伤害源, die() 已将其写入 lastDamageSource
                .withOptionalParameter(BukkitLootContextParameters.DAMAGE_SOURCE, LivingEntityProxy.INSTANCE.getLastDamageSource(bukkitEntity.minecraftEntity()));
        Player causingPlayer = null;
        float luck = 1.0f;
        Entity causingBukkitEntity = event.getDamageSource().getCausingEntity();
        if (causingBukkitEntity != null) {
            BukkitEntity adapted = BukkitAdaptor.adapt(causingBukkitEntity);
            builder.withParameter(DirectContextParameters.ENTITY, adapted);
            if (adapted instanceof Player player) {
                causingPlayer = player;
                luck = (float) causingPlayer.luck();
                Item itemInHand = causingPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                builder.withParameter(DirectContextParameters.PLAYER, player)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, ItemUtils.isEmpty(itemInHand) ? null : itemInHand);
            }
        }
        LootContext lootContext = new LootContext(world, causingPlayer, luck, builder.build());
        LootOutcome outcome = LootManager.eval(sources, lootContext);
        if (!outcome.matched()) return;
        if (outcome.overwriteItems()) {
            event.getDrops().clear();
        }
        if (outcome.overwriteExperience()) {
            event.setDroppedExp(0);
        }
        for (Item item : outcome.items()) {
            world.dropItemNaturally(position, item);
        }
    }
}
