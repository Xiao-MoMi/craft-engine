package net.momirealms.craftengine.bukkit.entity.projectile;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.scheduler.impl.FoliaTask;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.projectile.CustomProjectile;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
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
        Bukkit.getPluginManager().registerEvents(this, this.plugin.bootstrap());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public Optional<CustomProjectile> projectileByEntityId(int entityId) {
        return Optional.ofNullable(this.projectiles.get(entityId));
    }

    @EventHandler(ignoreCancelled = true,  priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        handleProjectileLoad(projectile);
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
        ItemStack item = event.getItem();
        String type = getType(item);
        if (type == null) return;
        if (type.equals("bow") || type.equals("spear")) {
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
        if (type.equals("bow")) {
            if (ticksHeldFor < 3) return;
            // player.sendMessage("可以投出自定义弓: " + item.getType() + " 持续 " + ticksHeldFor + " 刻");
        } else if (type.equals("trident")) {
            if (ticksHeldFor < 10) return;
            // player.sendMessage("可以投出自定义三叉戟: " + item.getType() + " 持续 " + ticksHeldFor + " 刻");
            Object nmsItemStack = FastNMS.INSTANCE.field$CraftItemStack$handle(item);
            Object nmsServerLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld());
            Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(player);
            boolean success = TridentRelease.releaseUsing(nmsItemStack, nmsServerLevel, nmsEntity);
            // player.sendMessage("释放成功: " + success);
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
        private boolean injected;

        public ProjectileInjectTask(Projectile projectile) {
            this.projectile = projectile;
            if (VersionHelper.isFolia()) {
                this.task = new FoliaTask(projectile.getScheduler().runAtFixedRate(plugin.bootstrap(), (t) -> this.run(), () -> {}, 1, 1));
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
                Object trackedEntity = FastNMS.INSTANCE.field$Entity$trackedEntity(nmsEntity);
                if (trackedEntity == null) {
                    return;
                }
                Object serverEntity = FastNMS.INSTANCE.filed$ChunkMap$TrackedEntity$serverEntity(trackedEntity);
                if (serverEntity == null) {
                    return;
                }
                try {
                    Reflections.field$ServerEntity$updateInterval.set(serverEntity, 1);
                    this.injected = true;
                } catch (ReflectiveOperationException e) {
                    plugin.logger().warn("Failed to update server entity tracking interval", e);
                }
            }
            if (canSpawnParticle(nmsEntity)) {
                this.projectile.getWorld().spawnParticle(ParticleUtils.BUBBLE, this.projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0);
            }
        }

        private static boolean canSpawnParticle(Object nmsEntity) {
            if (!FastNMS.INSTANCE.field$Entity$wasTouchingWater(nmsEntity)) return false;
            if (Reflections.clazz$AbstractArrow.isInstance(nmsEntity)) {
                return !FastNMS.INSTANCE.method$AbstractArrow$isInGround(nmsEntity);
            }
            return true;
        }
    }

    public static BukkitProjectileManager instance() {
        return instance;
    }
}
