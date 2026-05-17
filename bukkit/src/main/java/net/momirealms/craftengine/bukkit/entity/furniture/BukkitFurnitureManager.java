package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.furniture.hitbox.InteractionFurnitureHitboxConfig;
import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.WorldUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.furniture.tick.TickingFurnitureImpl;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.EntityLookupProxy;
import net.momirealms.craftengine.proxy.paper.world.ChunkEntitySlicesProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class BukkitFurnitureManager extends AbstractFurnitureManager {
    public static final NamespacedKey FURNITURE_KEY = KeyUtils.toNamespacedKey(FurnitureManager.FURNITURE_KEY);
    public static final NamespacedKey FURNITURE_EXTRA_DATA_KEY = KeyUtils.toNamespacedKey(FurnitureManager.FURNITURE_EXTRA_DATA_KEY);
    public static final NamespacedKey FURNITURE_COLLISION = KeyUtils.toNamespacedKey(FurnitureManager.FURNITURE_COLLISION);
    private static BukkitFurnitureManager instance;

    public static Class<?> COLLISION_ENTITY_CLASS = Interaction.class;
    public static Object NMS_COLLISION_ENTITY_TYPE = EntityTypeProxy.INTERACTION;
    public static ColliderType COLLISION_ENTITY_TYPE = ColliderType.INTERACTION;

    private final BukkitCraftEngine plugin;

    private final Map<Integer, BukkitFurniture> byMetaEntityId = new ConcurrentHashMap<>(256, 0.5f);
    private final Map<Integer, BukkitFurniture> byInteractableEntityId = new ConcurrentHashMap<>(512, 0.5f);
    private final Map<Integer, BukkitFurniture> byColliderEntityId = new ConcurrentHashMap<>(512, 0.5f);
    // Event listeners
    private final FurnitureEventListener furnitureEventListener;

    public static BukkitFurnitureManager instance() {
        return instance;
    }

    public BukkitFurnitureManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.furnitureEventListener = new FurnitureEventListener(this, plugin.worldManager());
    }

    @Override
    public Furniture place(WorldPosition position, FurnitureDefinition furniture, FurniturePersistentData dataAccessor, boolean playSound) {
        return this.place(LocationUtils.toLocation(position), furniture, dataAccessor, playSound, null);
    }

    public BukkitFurniture place(Location location, FurnitureDefinition furniture, FurniturePersistentData data, boolean playSound, @Nullable net.momirealms.craftengine.core.entity.player.Player player) {
        Entity furnitureEntity = EntityUtils.spawnEntity(location.getWorld(), location, EntityType.ITEM_DISPLAY, entity -> {
            ItemDisplay display = (ItemDisplay) entity;
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING, furniture.id().toString());
            try {
                display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY, data.toBytes());
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to set furniture PDC for " + furniture.id().toString(), e);
            }
            handleMetaEntityDuringChunkLoad(display);
        });
        if (playSound) {
            SoundData sound = furniture.settings().sounds().placeSound();
            location.getWorld().playSound(location, sound.id().toString(), SoundCategory.BLOCKS, sound.volume().get(), sound.pitch().get());
        }
        BukkitFurniture furnitureInstance = loadedFurnitureByMetaEntityId(furnitureEntity.getEntityId());
        if (furnitureInstance != null) {
            furnitureInstance.controller.onPlace(player);
        }

        return furnitureInstance;
    }

    @Override
    public void delayedInit() {
        super.delayedInit();

        // 确定碰撞箱实体类型
        COLLISION_ENTITY_TYPE = Config.colliderType();
        COLLISION_ENTITY_CLASS = Config.colliderType() == ColliderType.INTERACTION ? Interaction.class : Boat.class;
        NMS_COLLISION_ENTITY_TYPE = Config.colliderType() == ColliderType.INTERACTION ? EntityTypeProxy.INTERACTION : EntityTypeProxy.OAK_BOAT;

        // 注册事件
        Bukkit.getPluginManager().registerEvents(this.furnitureEventListener, this.plugin.javaPlugin());

        // 对世界上已有实体的记录
        if (VersionHelper.isFolia) {
            BiConsumer<Entity, Runnable> taskExecutor = (entity, runnable) -> entity.getScheduler().run(this.plugin.javaPlugin(), (t) -> runnable.run(), () -> {});
            for (World world : Bukkit.getWorlds()) {
                List<Entity> entities = world.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof ItemDisplay display) {
                        taskExecutor.accept(entity, () -> handleMetaEntityDuringChunkLoad(display));
                    } else if (entity instanceof Interaction interaction) {
                        taskExecutor.accept(entity, () -> handleCollisionEntityDuringChunkLoad(interaction));
                    } else if (entity instanceof Boat boat) {
                        taskExecutor.accept(entity, () -> handleCollisionEntityDuringChunkLoad(boat));
                    }
                }
            }
        } else {
            for (World world : Bukkit.getWorlds()) {
                List<Entity> entities = world.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof ItemDisplay display) {
                        handleMetaEntityDuringChunkLoad(display);
                    } else if (entity instanceof Interaction interaction) {
                        handleCollisionEntityDuringChunkLoad(interaction);
                    } else if (entity instanceof Boat boat) {
                        handleCollisionEntityDuringChunkLoad(boat);
                    }
                }
            }
        }
    }

    @Override
    public void disable() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ItemDisplay itemDisplay) {
                    handleMetaEntityUnload(itemDisplay, true);
                } else if (BukkitFurnitureManager.COLLISION_ENTITY_CLASS.isInstance(entity)) {
                    handleCollisionEntityUnload(entity);
                }
            }
        }
        super.disable();
        HandlerList.unregisterAll(this.furnitureEventListener);
        unload();
    }

    @Override
    public boolean isFurnitureMetaEntity(int entityId) {
        return this.byMetaEntityId.containsKey(entityId);
    }

    @Nullable
    @Override
    public BukkitFurniture loadedFurnitureByMetaEntityId(int entityId) {
        return this.byMetaEntityId.get(entityId);
    }

    @Nullable
    @Override
    public BukkitFurniture loadedFurnitureByInteractableEntityId(int entityId) {
        return this.byInteractableEntityId.get(entityId);
    }

    @Nullable
    @Override
    public BukkitFurniture loadedFurnitureByColliderEntityId(int entityId) {
        return this.byColliderEntityId.get(entityId);
    }

    // 当元数据实体被卸载了
    void handleMetaEntityUnload(ItemDisplay entity, boolean isStopping) {
        // 不是持久化的
        if (!entity.isPersistent()) {
            return;
        }
        int id = entity.getEntityId();
        BukkitFurniture furniture = this.byMetaEntityId.get(id);
        if (furniture != null) {

            // 标记无效
            this.invalidateFurniture(furniture, isStopping);

            // 区块还在加载的时候，就重复卸载了。为极其特殊情况
            if (!isStopping) {
                Location location = entity.getLocation();
                Object entityLookup = WorldUtils.getEntityLookup(location.getWorld());
                Object slices = EntityLookupProxy.INSTANCE.getChunk(entityLookup, location.getBlockX() >> 4, location.getBlockZ() >> 4);
                boolean isPreventing = slices != null && ChunkEntitySlicesProxy.INSTANCE.isPreventingStatusUpdates(slices);
                if (!isPreventing) {
                    furniture.destroySeats();
                }
            }

            // 触发行为卸载
            try {
                furniture.controller.onUnload(isStopping);
            } finally {
                furniture.saveIfDirty();
            }
        }
    }

    // 保险起见，collision实体卸载也移除一下
    void handleCollisionEntityUnload(Entity entity) {
        int id = entity.getEntityId();
        this.byColliderEntityId.remove(id);
    }

    // 检查这个区块的实体是否已经被加载了
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isEntitiesLoaded(Location location) {
        CEWorld ceWorld = this.plugin.worldManager().getWorld(location.getWorld());
        CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (ceChunk == null) return false;
        return ceChunk.isEntitiesLoaded();
    }

    void handleMetaEntityDuringChunkLoad(ItemDisplay entity) {
        // 实体可能不是持久的
        if (!entity.isPersistent()) {
            return;
        }

        // 获取家具pdc
        String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (id == null) return;

        // 处理无效的家具
        if (Config.handleInvalidFurniture()) {
            String mapped = Config.furnitureMappings().get(id);
            if (mapped != null) {
                if (mapped.isEmpty()) {
                    entity.remove();
                    return;
                } else {
                    id = mapped;
                    entity.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, id);
                }
            }
        }

        // 获取家具配置
        Key key = Key.of(id);
        Optional<FurnitureDefinition> optionalFurniture = furnitureById(key);
        if (optionalFurniture.isEmpty()) return;

        // 只对1.20.2及以上生效，1.20.1比较特殊
        if (!VersionHelper.isOrAbove1_20_2) {
            return;
        }

        // 已经在其他事件里加载过了
        FurnitureDefinition furnitureDefinition = optionalFurniture.get();
        BukkitFurniture previous = this.byMetaEntityId.get(entity.getEntityId());
        if (previous != null) return;

        // 创建新的家具
        BukkitFurniture furnitureInstance = createFurnitureInstance(entity, furnitureDefinition);
        CompoundTag data = (CompoundTag) Optional.ofNullable(furnitureInstance.persistentData.getTag(FurniturePersistentData.CUSTOM_DATA)).orElseGet(CompoundTag::new);
        furnitureInstance.controller.loadCustomData(data);
        furnitureInstance.controller.onLoad();
    }

    @SuppressWarnings("deprecation")
    void handleMetaEntityAfterChunkLoad(ItemDisplay entity) {
        // 实体可能不是持久的
        if (!entity.isPersistent()) {
            return;
        }

        // 获取家具pdc
        String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (id == null) return;

        // 这个区块还处于加载实体中，这个时候不处理（1.20.1需要特殊处理）
        Location location = entity.getLocation();
        if (VersionHelper.isOrAbove1_20_2 && !isEntitiesLoaded(location)) {
            return;
        }

        // 获取家具配置
        Key key = Key.of(id);
        Optional<FurnitureDefinition> optionalFurniture = furnitureById(key);
        if (optionalFurniture.isEmpty()) return;

        // 已经在其他事件里加载过了
        FurnitureDefinition furnitureDefinition = optionalFurniture.get();
        BukkitFurniture previous = this.byMetaEntityId.get(entity.getEntityId());
        if (previous != null) return;

        createFurnitureInstance(entity, furnitureDefinition);

        // 补发一次包，修复
        for (Player player : entity.getTrackedPlayers()) {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
            if (serverPlayer == null) continue;
            serverPlayer.sendPacket(ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                    entity.getEntityId(), entity.getUniqueId(), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(),
                    EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
            ), false);
        }
    }

    void handleCollisionEntityAfterChunkLoad(Entity entity) {
        // 如果是碰撞实体，那么就忽略
        if (CraftEntityProxy.INSTANCE.getEntity(entity) instanceof CollisionEntity) {
            return;
        }
        // 看看有没有碰撞实体的pdc
        Byte flag = entity.getPersistentDataContainer().get(FURNITURE_COLLISION, PersistentDataType.BYTE);
        if (flag == null || flag != 1) {
            return;
        }
        // 实体未加载
        Location location = entity.getLocation();
        if (!isEntitiesLoaded(location)) {
            return;
        }

        // 移除被WorldEdit错误复制的碰撞实体
        runSafeEntityOperation(location.getChunk(), entity::remove);
    }

    public void handleCollisionEntityDuringChunkLoad(Entity collisionEntity) {
        // faster
        if (CraftEntityProxy.INSTANCE.getEntity(collisionEntity) instanceof CollisionEntity) {
            collisionEntity.remove();
            return;
        }

        // not a collision entity
        Byte flag = collisionEntity.getPersistentDataContainer().get(FURNITURE_COLLISION, PersistentDataType.BYTE);
        if (flag == null || flag != 1) {
            return;
        }

        collisionEntity.remove();
    }

    private FurniturePersistentData getFurnitureDataAccessor(Entity baseEntity) {
        byte[] extraData = baseEntity.getPersistentDataContainer().get(FURNITURE_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY);
        if (extraData == null) return new FurniturePersistentData(null);
        try {
            return FurniturePersistentData.fromBytes(extraData);
        } catch (IOException e) {
            // 损坏了？一般不会
            return new FurniturePersistentData(null);
        }
    }

    // 创建家具实例，并初始化碰撞实体
    private BukkitFurniture createFurnitureInstance(ItemDisplay display, FurnitureDefinition furniture) {
        BukkitFurniture bukkitFurniture = new BukkitFurniture(display, furniture, getFurnitureDataAccessor(display));
        initFurniture(bukkitFurniture);
        Location location = display.getLocation();
        runSafeEntityOperation(location.getChunk(), () -> {
            bukkitFurniture.addCollidersToWorld();
            for (FurnitureElement element : bukkitFurniture.elements()) {
                element.activate();
            }
        });
        return bukkitFurniture;
    }

    void initFurniture(BukkitFurniture furniture) {
        int entityId = furniture.entityId();
        this.byMetaEntityId.put(entityId, furniture);
        this.byInteractableEntityId.put(entityId, furniture);
        for (int id : furniture.interactableEntityIds()) {
            this.byInteractableEntityId.put(id, furniture);
        }
        for (Collider collisionEntity : furniture.colliders()) {
            this.byColliderEntityId.put(collisionEntity.entityId(), furniture);
        }
        if (!this.syncTickers.containsKey(entityId)) {
            FurnitureTicker<FurnitureController> ticker = furniture.controller.createFurnitureTicker();
            if (ticker != null) {
                TickingFurnitureImpl<FurnitureController> tickingFurniture = new TickingFurnitureImpl<>(furniture, ticker);
                this.syncTickers.put(entityId, tickingFurniture);
                if (VersionHelper.isFolia) {
                    furniture.bukkitEntity().getScheduler().runAtFixedRate(this.plugin.javaPlugin(), (t) -> {
                        if (tickingFurniture.isValid()) {
                            tickingFurniture.tick();
                        }
                    }, () -> this.syncTickers.remove(tickingFurniture.entityId()), 1, 1);
                } else {
                    this.addSyncFurnitureTicker(tickingFurniture);
                }
            }
        }
        if (!this.asyncTickers.containsKey(entityId)) {
            FurnitureTicker<FurnitureController> ticker = furniture.controller.createAsyncFurnitureTicker();
            if (ticker != null) {
                TickingFurnitureImpl<FurnitureController> tickingFurniture = new TickingFurnitureImpl<>(furniture, ticker);
                this.asyncTickers.put(entityId, tickingFurniture);
                this.addAsyncFurnitureTicker(tickingFurniture);
            }
        }
    }

    void invalidateFurniture(BukkitFurniture furniture, boolean isStopping) {
        int entityId = furniture.entityId();
        // 移除entity id映射
        this.byMetaEntityId.remove(entityId);
        this.byInteractableEntityId.remove(entityId);
        for (int id : furniture.interactableEntityIds()) {
            this.byInteractableEntityId.remove(id);
        }
        for (Collider collisionEntity : furniture.colliders()) {
            if (!isStopping) {
                tryRemoveCollider(collisionEntity);
            }
            this.byColliderEntityId.remove(collisionEntity.entityId());
        }
        for (FurnitureElement element : furniture.elements()) {
            element.deactivate();
        }
    }

    private void tryRemoveCollider(Collider collider) {
        Object entity = collider.handle();
        Object level = EntityProxy.INSTANCE.getLevel(entity);
        Object entityLookup;
        if (VersionHelper.isOrAbove1_21) {
            entityLookup = LevelProxy.INSTANCE.moonrise$getEntityLookup(level);
        } else {
            entityLookup = ServerLevelProxy.INSTANCE.getEntityLookup(level);
        }
        if (!EntityLookupProxy.INSTANCE.canRemoveEntity(entityLookup, entity)) return;
        collider.destroy();
    }

    private void runSafeEntityOperation(Chunk chunk, Runnable action) {
        if (!chunk.isLoaded()) return;
        Object world = CraftWorldProxy.INSTANCE.getWorld(chunk.getWorld());
        Object entityLookup;
        if (VersionHelper.isOrAbove1_21) {
            entityLookup = LevelProxy.INSTANCE.moonrise$getEntityLookup(world);
        } else {
            entityLookup = ServerLevelProxy.INSTANCE.getEntityLookup(world);
        }
        Object slices = EntityLookupProxy.INSTANCE.getChunk(entityLookup, chunk.getX(), chunk.getZ());
        boolean preventChange = slices != null && ChunkEntitySlicesProxy.INSTANCE.isPreventingStatusUpdates(slices);
        if (preventChange) {
            this.plugin.scheduler().platform().runLater(action, 1, chunk.getWorld(), chunk.getX(), chunk.getZ());
        } else {
            action.run();
        }
    }

    @Override
    protected FurnitureHitBoxConfig<?> defaultHitBox() {
        return InteractionFurnitureHitboxConfig.DEFAULT;
    }
}
