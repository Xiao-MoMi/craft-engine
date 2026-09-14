package net.momirealms.craftengine.bukkit.entity.projectile;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.BlockDispenseProjectileEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.network.listener.game.LevelEventListener;
import net.momirealms.craftengine.bukkit.plugin.network.listener.game.SoundListener;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.core.entity.projectile.ProjectileManager;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.entity.projectile.ProjectileSounds;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.enchantment.EnchantmentKeys;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Tristate;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ChunkMapProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.projectile.AbstractArrowProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.projectile.FireworkRocketEntityProxy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitProjectileManager implements Listener, ProjectileManager {
    static final NamespacedKey PROJECTILE_ITEM = new NamespacedKey("craftengine", "projectile_item");
    private static BukkitProjectileManager instance;
    private final BukkitCraftEngine plugin;
    private final PaperProjectileEventListener paperProjectileEventListener;
    // 会被netty线程访问
    final Map<Integer, BukkitCustomProjectile> projectiles = new ConcurrentHashMap<>();

    public BukkitProjectileManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.paperProjectileEventListener = VersionHelper.hasPaperPatch ? new PaperProjectileEventListener(this) : null;
        instance = this;
    }

    public static BukkitProjectileManager instance() {
        return instance;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        if (this.paperProjectileEventListener != null) Bukkit.getPluginManager().registerEvents(this.paperProjectileEventListener, this.plugin.javaPlugin());
        for (World world : Bukkit.getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                if (entity instanceof Projectile projectile) {
                    this.plugin.scheduler().platform().run(() -> handleProjectileLoad(projectile, false), null, projectile);
                }
            }
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (this.paperProjectileEventListener != null) HandlerList.unregisterAll(this.paperProjectileEventListener);
    }

    @Override
    public Optional<BukkitCustomProjectile> projectileByEntityId(int entityId) {
        return Optional.ofNullable(this.projectiles.get(entityId));
    }

    ItemStack getItemFromProjectile(Projectile projectile, boolean readPdc) {
        if (projectile instanceof ThrowableProjectile throwableProjectile) {
            return throwableProjectile.getItem();
        } else if (projectile instanceof AbstractArrow abstractArrow) {
            return ItemStackUtils.getBukkitStack(AbstractArrowProxy.INSTANCE.getPickupItem(CraftEntityProxy.INSTANCE.getEntity(abstractArrow)));
        } else if (projectile instanceof Firework firework) {
            return ItemStackUtils.getBukkitStack(FireworkRocketEntityProxy.INSTANCE.getItem(CraftEntityProxy.INSTANCE.getEntity(firework)));
        } else if (projectile instanceof SizedFireball sizedFireball) {
            return sizedFireball.getDisplayItem();
        }
        if (readPdc) {
            byte[] bytes = projectile.getPersistentDataContainer().get(PROJECTILE_ITEM, PersistentDataType.BYTE_ARRAY);
            if (bytes != null) {
                return this.plugin.itemManager().fromBytes(bytes).getBukkitItem();
            }
        }
        return null;
    }

    // 发射器里射出的弹射物，这里特指风弹
    @EventHandler
    public void onDispenseProjectile(BlockDispenseProjectileEvent event) {
        Projectile projectile = event.getProjectile();
        ItemStack storedItem = getItemFromProjectile(projectile, false);
        if (storedItem == null) {
            projectile.getPersistentDataContainer().set(PROJECTILE_ITEM, PersistentDataType.BYTE_ARRAY, BukkitItemManager.instance().wrap(event.getItem()).toBytes());
            handleProjectileLoad(projectile, true);
        }
    }

    // 可能是玩家发射的也可能是发射器发射的
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        handleProjectileLoad(event.getEntity(), true);
    }

    // 穿过传送门需要销毁
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        this.projectiles.remove(event.getEntity().getEntityId());
    }

    @EventHandler(ignoreCancelled = true,  priority = EventPriority.HIGHEST)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Projectile projectile) {
                handleProjectileLoad(projectile, false);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        BukkitCustomProjectile customProjectile = this.projectiles.get(projectile.getEntityId());
        if (customProjectile == null) return;
        ProjectileMeta meta = customProjectile.metadata();
        ProjectileSounds sounds = meta.sounds();
        if (sounds == null) return;

        Block block = event.getHitBlock();
        if (block != null) {
            ProjectileSounds.TargetBasedSound targetBasedSound = sounds.hitBlockSound();
            if (targetBasedSound != null) {
                SoundData soundData = targetBasedSound.get(BukkitAdaptor.adapt(block).id());
                if (soundData != null) {
                    Location location = projectile.getLocation();
                    location.getWorld().playSound(location, soundData.id().asString(), SoundCategory.NEUTRAL, soundData.volume().get(), soundData.pitch().get());
                }
            }
        }

        Entity hitEntity = event.getHitEntity();
        if (hitEntity != null) {
            ProjectileSounds.TargetBasedSound targetBasedSound = sounds.hitEntitySound();
            if (targetBasedSound != null) {
                SoundData soundData = targetBasedSound.get(EntityUtils.getEntityType(hitEntity));
                if (soundData != null) {
                    Location location = projectile.getLocation();
                    location.getWorld().playSound(location, soundData.id().asString(), SoundCategory.NEUTRAL, soundData.volume().get(), soundData.pitch().get());
                }
            }
        }

        if (meta.removeOnHit()) {
            projectile.remove();
        }
    }

    public void handleProjectileUnload(Projectile projectile) {
        this.projectiles.remove(projectile.getEntityId());
    }

    public void handleProjectileLoad(Projectile projectile, boolean launch) {
        if (this.projectiles.containsKey(projectile.getEntityId())) return;
        ItemStack projectileItem = getItemFromProjectile(projectile, true);
        if (projectileItem == null) return;
        Item wrapped = this.plugin.itemManager().wrap(projectileItem);
        if (ItemUtils.isEmpty(wrapped)) return;
        wrapped.getDefinition().ifPresent(it -> {
            ProjectileMeta meta = it.settings().projectileMeta();
            if (meta != null) {
                BukkitCustomProjectile customProjectile = new BukkitCustomProjectile(meta, projectile, wrapped);
                this.projectiles.put(projectile.getEntityId(), customProjectile);
                new ProjectileInjectTask(projectile, wrapped.getEnchantment(EnchantmentKeys.LOYALTY).isEmpty());
                Tristate gravity = meta.gravity();
                if (gravity != Tristate.UNDEFINED) {
                    projectile.setGravity(gravity.asBoolean());
                }
                if (meta.velocity() != 1) {
                    projectile.setVelocity(projectile.getVelocity().multiply(meta.velocity()));
                }
                if (projectile instanceof AbstractArrow abstractArrow) {
                    if (meta.damage() >= 0) {
                        abstractArrow.setDamage(meta.damage());
                    }
                    if (meta.pierceLevel() >= 0) {
                        abstractArrow.setPierceLevel(Math.min(127, meta.pierceLevel()));
                    }
                }
                ProjectileSounds sounds = meta.sounds();
                // 如果有自定义声音，就让雪豹闭嘴
                if (sounds != null) {
                    projectile.setSilent(true);
                    if (launch) {
                        Location location = projectile.getLocation();
                        Location playerLocation = null;
                        if (projectile.getShooter() instanceof Player thrower) {
                            playerLocation = thrower.getLocation();
                        }
                        if (projectile instanceof SmallFireball smallFireball) {
                            Vector velocity = smallFireball.getVelocity();
                            Direction approximateNearest = Direction.getApproximateNearest(velocity.getX(), velocity.getY(), velocity.getZ()).opposite();
                            Location added = location.add(new Vector(approximateNearest.stepX() * 0.75, approximateNearest.stepY() * 0.75, approximateNearest.stepZ() * 0.75));
                            LevelEventListener.addTempIgnoredEvent(added, WorldEvents.BLAZE_SHOOTS);
                        } else {
                            SoundListener.addTempIgnoredSound(location);
                            if (playerLocation != null) {
                                SoundListener.addTempIgnoredSound(playerLocation);
                            }
                        }
                        SoundData throwSound = sounds.throwSound();
                        if (throwSound != null) {
                            location.getWorld().playSound(location, throwSound.id().asString(), SoundCategory.NEUTRAL, throwSound.volume().get(), throwSound.pitch().get());
                        }
                    }
                }
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
            this.task = plugin.scheduler().platform().runRepeating(this, null, 1, 1, projectile);
        }

        @Override
        public void run() {
            if (!this.projectile.isValid()) {
                this.task.cancel();
                BukkitProjectileManager.this.projectiles.remove(this.projectile.getEntityId());
                return;
            }

            Object nmsEntity = CraftEntityProxy.INSTANCE.getEntity(this.projectile);
            // 获取server entity
            if (this.cachedServerEntity == null) {
                if (VersionHelper.hasPaperPatch) {
                    Object trackedEntity = EntityProxy.INSTANCE.getTrackedEntity(nmsEntity);
                    if (trackedEntity == null) return;
                    Object serverEntity = ChunkMapProxy.TrackedEntityProxy.INSTANCE.getServerEntity(trackedEntity);
                    if (serverEntity == null) return;
                    this.cachedServerEntity = serverEntity;
                } else {
                    Int2ObjectMap<Object> entityMap = ChunkMapProxy.INSTANCE.getEntityMap(ServerChunkCacheProxy.INSTANCE.getChunkMap(ServerLevelProxy.INSTANCE.getChunkSource(EntityProxy.INSTANCE.getLevel(nmsEntity))));
                    Object trackedEntity = entityMap.get(this.projectile.getEntityId());
                    if (trackedEntity == null) return;
                    Object serverEntity = ChunkMapProxy.TrackedEntityProxy.INSTANCE.getServerEntity(trackedEntity);
                    if (serverEntity == null) return;
                    this.cachedServerEntity = serverEntity;
                }
            }

            if (!AbstractArrowProxy.CLASS.isInstance(nmsEntity)) {
                updateProjectileUpdateInterval(1);
            } else if (!this.checkInGround) {
                updateProjectileUpdateInterval(1);
                if (EntityProxy.INSTANCE.isWasTouchingWater(nmsEntity)) {
                    this.projectile.getWorld().spawnParticle(ParticleUtils.BUBBLE, this.projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0);
                }
            } else {
                boolean inGround;
                if (VersionHelper.isOrAbove1_21_2) {
                    inGround = AbstractArrowProxy.INSTANCE.isInGround$0(nmsEntity);
                } else {
                    inGround = AbstractArrowProxy.INSTANCE.isInGround$1(nmsEntity);
                }
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
            if (this.lastInjectedInterval == updateInterval || this.cachedServerEntity == null) return;
            ServerEntityProxy.INSTANCE.setUpdateInterval(this.cachedServerEntity, updateInterval);
            this.lastInjectedInterval = updateInterval;
        }

        private static boolean canSpawnParticle(Object nmsEntity, boolean inGround) {
            if (!EntityProxy.INSTANCE.isWasTouchingWater(nmsEntity)) return false;
            if (AbstractArrowProxy.CLASS.isInstance(nmsEntity)) {
                return !inGround;
            }
            return true;
        }
    }
}
