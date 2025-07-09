package net.momirealms.craftengine.bukkit.entity.projectile;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.scheduler.impl.FoliaTask;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitProjectileManager implements Listener, ProjectileManager {
    private static BukkitProjectileManager instance;
    private final BukkitCraftEngine plugin;
    // 会被netty线程访问
    private final Map<Integer, BukkitCustomProjectile> projectiles = new ConcurrentHashMap<>();

    public BukkitProjectileManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        if (VersionHelper.isFolia()) {
            for (World world : Bukkit.getWorlds()) {
                List<Entity> entities = world.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof Projectile projectile) {
                        projectile.getScheduler().run(this.plugin.javaPlugin(), (t) -> handleProjectileLoad(projectile), () -> {});
                    }
                }
            }
        } else {
            for (World world : Bukkit.getWorlds()) {
                List<Entity> entities = world.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof Projectile projectile) {
                        handleProjectileLoad(projectile);
                    }
                }
            }
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public Optional<BukkitCustomProjectile> projectileByEntityId(int entityId) {
        return Optional.ofNullable(this.projectiles.get(entityId));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        handleProjectileLoad(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        this.projectiles.remove(event.getEntity().getEntityId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Projectile projectile) {
            handleProjectileLoad(projectile);
        }
    }

    @EventHandler(ignoreCancelled = true,  priority = EventPriority.HIGHEST)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Projectile projectile) {
                handleProjectileLoad(projectile);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        this.projectiles.remove(event.getEntity().getEntityId());
    }

    private void handleProjectileLoad(Projectile projectile) {
        ItemStack projectileItem;
        if (projectile instanceof ThrowableProjectile throwableProjectile) {
            projectileItem = throwableProjectile.getItem();
        } else if (projectile instanceof Arrow arrow) {
            projectileItem = arrow.getItemStack();
        } else {
            return;
        }
        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(projectileItem);
        if (wrapped == null) return;
        wrapped.getCustomItem().ifPresent(it -> {
            ProjectileMeta meta = it.settings().projectileMeta();
            if (meta != null) {
                BukkitCustomProjectile customProjectile = new BukkitCustomProjectile(meta, projectile, wrapped);
                this.projectiles.put(projectile.getEntityId(), customProjectile);
                new ProjectileInjectTask(projectile, !projectileItem.getItemMeta().hasEnchant(Enchantment.LOYALTY));
            }
        });
    }

    public class ProjectileInjectTask implements Runnable {
        private final Projectile projectile;
        private final SchedulerTask task;
        private final boolean checkInGround;
        private Object cachedServerEntity;
        private int lastInjectedInterval = 0;

        public ProjectileInjectTask(Projectile projectile, boolean checkInGround) {
            this.projectile = projectile;
            this.checkInGround = checkInGround;
            if (VersionHelper.isFolia()) {
                this.task = new FoliaTask(projectile.getScheduler().runAtFixedRate(plugin.javaPlugin(), (t) -> this.run(), () -> {}, 1, 1));
            } else {
                this.task = plugin.scheduler().sync().runRepeating(this, 1, 1);
            }
        }

        @Override
        public void run() {
            if (!this.projectile.isValid()) {
                this.task.cancel();
                return;
            }

            Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(this.projectile);
            // 获取server entity
            if (this.cachedServerEntity == null) {
                Object trackedEntity = FastNMS.INSTANCE.field$Entity$trackedEntity(nmsEntity);
                if (trackedEntity == null) return;
                Object serverEntity = FastNMS.INSTANCE.field$ChunkMap$TrackedEntity$serverEntity(trackedEntity);
                if (serverEntity == null) return;
                this.cachedServerEntity = serverEntity;
            }

            if (!CoreReflections.clazz$AbstractArrow.isInstance(nmsEntity)) {
                updateProjectileUpdateInterval(1);
            } else if (!this.checkInGround) {
                updateProjectileUpdateInterval(1);
                if (FastNMS.INSTANCE.field$Entity$wasTouchingWater(nmsEntity)) {
                    this.projectile.getWorld().spawnParticle(ParticleUtils.BUBBLE, this.projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0);
                }
            } else {
                boolean inGround = FastNMS.INSTANCE.method$AbstractArrow$isInGround(nmsEntity);
                if (canSpawnParticle(nmsEntity, inGround)) {
                    this.projectile.getWorld().spawnParticle(ParticleUtils.BUBBLE, this.projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0);
                }
                if (inGround) {
                    updateProjectileUpdateInterval(Integer.MAX_VALUE);
                } else {
                    updateProjectileUpdateInterval(1);
                }
            }
        }

        private void updateProjectileUpdateInterval(int updateInterval) {
            if (this.lastInjectedInterval == updateInterval) return;
            try {
                CoreReflections.methodHandle$ServerEntity$updateIntervalSetter.invokeExact(this.cachedServerEntity, updateInterval);
                this.lastInjectedInterval = updateInterval;
            } catch (Throwable e) {
                BukkitProjectileManager.this.plugin.logger().warn("Failed to update server entity update interval for " + this.projectile.getType().getKey() + "[" + this.projectile.getUniqueId() + "]", e);
            }
        }

        private static boolean canSpawnParticle(Object nmsEntity, boolean inGround) {
            if (!FastNMS.INSTANCE.field$Entity$wasTouchingWater(nmsEntity)) return false;
            if (CoreReflections.clazz$AbstractArrow.isInstance(nmsEntity)) {
                return !inGround;
            }
            return true;
        }
    }

    public static BukkitProjectileManager instance() {
        return instance;
    }
}
