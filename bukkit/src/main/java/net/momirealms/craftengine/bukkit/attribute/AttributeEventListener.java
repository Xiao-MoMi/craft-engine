package net.momirealms.craftengine.bukkit.attribute;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.listener.AbstractListener;
import net.momirealms.craftengine.core.attribute.EntityAttributesSnapshot;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;

public final class AttributeEventListener extends AbstractListener {
    public static final NamespacedKey PROJECTILE_SHOOT_FORCE = new NamespacedKey("craftengine", "shoot_force");

    public AttributeEventListener() {
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootProjectile(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof LivingEntity livingEntity) {
            LivingEntityHolder holder = BukkitCraftEngine.instance().entityManager().getEntityHolder(livingEntity.getUniqueId());
            if (holder == null) {
                return;
            }
            holder.ifAttributesExist(attributes -> {
                EntityAttributesSnapshot snapshot = attributes.createSnapshot();
                BukkitAdaptor.adapt(projectile).setCustomData(EntityAttributesSnapshot.PROJECTILE_DATA_KEY, snapshot);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        Entity projectile = event.getProjectile();
        float force = event.getForce();
        if (!Float.isFinite(force)) {
            force = 1.0F;
        } else {
            force = Math.clamp(force, 0.0F, 1.0F);
        }
        projectile.getPersistentDataContainer().set(PROJECTILE_SHOOT_FORCE, PersistentDataType.FLOAT, force);
    }
}
