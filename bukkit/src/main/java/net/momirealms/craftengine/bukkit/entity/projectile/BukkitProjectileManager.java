package net.momirealms.craftengine.bukkit.entity.projectile;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.scheduler.impl.FoliaTask;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.core.entity.projectile.CustomProjectile;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitProjectileManager implements Listener, ProjectileManager {
    private static BukkitProjectileManager instance;
    private final BukkitCraftEngine plugin;
    private final Map<Integer, BukkitCustomProjectile> projectiles;

    public BukkitProjectileManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.projectiles = new ConcurrentHashMap<>();
        instance = this;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        for (World world : Bukkit.getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                if (entity instanceof Projectile projectile) {
                    handleProjectileLoad(projectile);
                }
            }
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public Optional<CustomProjectile> projectileByEntityId(int entityId) {
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
                this.projectiles.put(projectile.getEntityId(), new BukkitCustomProjectile(meta, projectile, wrapped));
                new ProjectileInjectTask(projectile);
            }
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerItemConsumeEvent event) {
        String type = getType(event.getItem());
        if (type == null) return;
        if (type.equals("bow") || type.equals("trident")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerStopUsingItem(PlayerStopUsingItemEvent event) {
        ItemStack item = event.getItem();
        String type = getType(item);
        if (type == null) return;
        int ticksHeldFor = event.getTicksHeldFor();
        Player player = event.getPlayer();
        if (type.equals("trident")) {
            if (ticksHeldFor < 10) return;
            Object nmsItemStack = FastNMS.INSTANCE.field$CraftItemStack$handle(item);
            Object nmsServerLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld());
            Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(player);
            TridentRelease.releaseUsing(nmsItemStack, nmsServerLevel, nmsEntity);
        } else if (type.equals("bow")) {
            if (ticksHeldFor < 3) return;
        }
    }

    @Nullable
    private String getType(ItemStack item) {
        Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(item);
        Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
        if (optionalCustomItem.isEmpty()) return null;
        CustomItem<ItemStack> customItem = optionalCustomItem.get();
        ProjectileMeta meta = customItem.settings().projectileMeta();
        if (meta == null) return null;
        return meta.type();
    }

    public class ProjectileInjectTask implements Runnable {
        private final Projectile projectile;
        private final SchedulerTask task;
        private Object cachedServerEntity;
        private boolean injected;
        private int lastInjectedInterval = -1;
        private boolean wasInGround;

        public ProjectileInjectTask(Projectile projectile) {
            this.projectile = projectile;
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
            if (!this.injected) {
                injectProjectile(nmsEntity, 1);
                this.injected = true;
                this.lastInjectedInterval = 1;
            }
            boolean inGround = FastNMS.INSTANCE.method$AbstractArrow$isInGround(nmsEntity);
            if (canSpawnParticle(nmsEntity, inGround)) {
                this.projectile.getWorld().spawnParticle(ParticleUtils.BUBBLE, this.projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0);
            }
            projectileByEntityId(this.projectile.getEntityId()).ifPresent(customProjectile -> {
                customProjectile.setInGroundTime(inGround ? customProjectile.inGroundTime() + 1 : 0);
                if (customProjectile.inGroundTime() > 5) {
                    if (lastInjectedInterval != Integer.MAX_VALUE) {
                        injectProjectile(nmsEntity, Integer.MAX_VALUE);
                    }
                } else if (!inGround && wasInGround) {
                    if (lastInjectedInterval != 1) {
                        injectProjectile(nmsEntity, 1);
                    }
                }
                this.wasInGround = inGround;
            });
        }

        private void injectProjectile(Object entity, int updateInterval) {
            if (this.cachedServerEntity == null) {
                Object trackedEntity = FastNMS.INSTANCE.field$Entity$trackedEntity(entity);
                if (trackedEntity == null) return;
                Object serverEntity = FastNMS.INSTANCE.filed$ChunkMap$TrackedEntity$serverEntity(trackedEntity);
                if (serverEntity == null) return;
                this.cachedServerEntity = serverEntity;
            }
            try {
                CoreReflections.handle$ServerEntity$updateIntervalSetter.invokeExact(this.cachedServerEntity, updateInterval);
                this.lastInjectedInterval = updateInterval;
            } catch (Throwable e) {
                plugin.logger().warn("Failed to update server entity tracking interval", e);
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
