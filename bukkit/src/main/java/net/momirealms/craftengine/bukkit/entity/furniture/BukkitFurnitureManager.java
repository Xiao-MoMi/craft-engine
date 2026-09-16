package net.momirealms.craftengine.bukkit.entity.furniture;

import ca.spottedleaf.concurrentutil.map.concurrent.ints.ConcurrentChainedInt2ReferenceHashTable;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.hitbox.InteractionFurnitureHitboxConfig;
import net.momirealms.craftengine.bukkit.entity.furniture.listener.FurnitureEventListener;
import net.momirealms.craftengine.bukkit.entity.furniture.listener.PaperFurnitureEventListener;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.furniture.tick.TickingFurnitureImpl;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
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
import java.util.Optional;
import java.util.function.BiConsumer;

public final class BukkitFurnitureManager extends AbstractFurnitureManager {
    public static final NamespacedKey FURNITURE_KEY = KeyUtils.toNamespacedKey(FurnitureManager.FURNITURE_KEY);
    public static final NamespacedKey FURNITURE_EXTRA_DATA_KEY = KeyUtils.toNamespacedKey(FurnitureManager.FURNITURE_EXTRA_DATA_KEY);
    public static final NamespacedKey FURNITURE_COLLISION = KeyUtils.toNamespacedKey(FurnitureManager.FURNITURE_COLLISION);
    private static BukkitFurnitureManager instance;

    public static Class<?> COLLISION_ENTITY_CLASS = Interaction.class;
    public static Object NMS_COLLISION_ENTITY_TYPE = EntityTypesProxy.INTERACTION;
    public static ColliderType COLLISION_ENTITY_TYPE = ColliderType.INTERACTION;

    private final BukkitCraftEngine plugin;

    // 同一个元数据实体会经过多个入口；以运行时 entityId 去重，不能用家具配置 ID 去重。
    // 先登记再添加 Collider，先撤销登记再移除 Collider，避免同步嵌套事件重复创建/卸载。
    // 并发哈希表供网络线程查询；它不意味着同一家具可跨 Folia 区域并发初始化。
    private final ConcurrentChainedInt2ReferenceHashTable<BukkitFurniture> byMetaEntityId = ConcurrentChainedInt2ReferenceHashTable.createWithCapacity(256, 0.5f);
    private final ConcurrentChainedInt2ReferenceHashTable<BukkitFurniture> byInteractableEntityId = ConcurrentChainedInt2ReferenceHashTable.createWithCapacity(512, 0.5f);
    private final ConcurrentChainedInt2ReferenceHashTable<BukkitFurniture> byColliderEntityId = ConcurrentChainedInt2ReferenceHashTable.createWithCapacity(512, 0.5f);
    // Event listeners
    private final FurnitureEventListener furnitureEventListener;
    private final PaperFurnitureEventListener paperFurnitureEventListener;

    public static BukkitFurnitureManager instance() {
        return instance;
    }

    public BukkitFurnitureManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.furnitureEventListener = new FurnitureEventListener(this, plugin.worldManager());
        this.paperFurnitureEventListener = VersionHelper.hasPaperPatch ? new PaperFurnitureEventListener(this) : null;
    }

    @Override
    public Furniture place(WorldPosition position, FurnitureDefinition furniture, FurniturePersistentData dataAccessor, boolean playSound) {
        return this.place(LocationUtils.toLocation(position), furniture, dataAccessor, playSound, null);
    }

    public BukkitFurniture place(Location location, FurnitureDefinition furniture, FurniturePersistentData data, boolean playSound, @Nullable net.momirealms.craftengine.core.entity.player.Player player) {
        // Bukkit spawn 的 consumer 在元数据实体加入世界之前执行。
        // 提前建立家具映射，使首次生成包就能被 AddEntityListener 识别；
        // 后续 Paper 单实体事件只会命中去重。各版本使用相同的家具初始化流程。
        Entity furnitureEntity = EntityUtils.spawnEntity(location.getWorld(), location, EntityType.ITEM_DISPLAY, entity -> {
            ItemDisplay display = (ItemDisplay) entity;
            FurniturePersistentData placedData = new FurniturePersistentData(data.copyTag());
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING, furniture.id().toString());
            try {
                display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY, placedData.toBytes());
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to set furniture PDC for " + furniture.id().toString(), e);
            }
            createFurnitureAndLoadBehavior(display, furniture, placedData, null);
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
        NMS_COLLISION_ENTITY_TYPE = Config.colliderType() == ColliderType.INTERACTION ? EntityTypesProxy.INTERACTION : EntityTypesProxy.OAK_BOAT;

        // 注册事件
        Bukkit.getPluginManager().registerEvents(this.furnitureEventListener, this.plugin.javaPlugin());
        if (this.paperFurnitureEventListener != null) Bukkit.getPluginManager().registerEvents(this.paperFurnitureEventListener, this.plugin.javaPlugin());

        // 插件启用时，已有实体的加载事件可能早已发生，需要主动扫描补登记。
        // 与区块批量加载共用从元数据实体恢复家具的入口。
        // Folia 中把每个实体的恢复提交给其 EntityScheduler，不能在启用线程直接增删实体。
        if (VersionHelper.hasFoliaPatch) {
            BiConsumer<Entity, Runnable> taskExecutor = (entity, runnable) -> entity.getScheduler().run(this.plugin.javaPlugin(), (t) -> runnable.run(), () -> {});
            for (World world : Bukkit.getWorlds()) {
                List<Entity> entities = world.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof ItemDisplay display) {
                        taskExecutor.accept(entity, () -> restoreFurnitureFromEntity(display));
                    } else if (entity instanceof Interaction interaction) {
                        taskExecutor.accept(entity, () -> removeStaleColliderEntity(interaction));
                    } else if (entity instanceof Boat boat) {
                        taskExecutor.accept(entity, () -> removeStaleColliderEntity(boat));
                    }
                }
            }
        } else {
            for (World world : Bukkit.getWorlds()) {
                List<Entity> entities = world.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof ItemDisplay display) {
                        restoreFurnitureFromEntity(display);
                    } else if (entity instanceof Interaction interaction) {
                        removeStaleColliderEntity(interaction);
                    } else if (entity instanceof Boat boat) {
                        removeStaleColliderEntity(boat);
                    }
                }
            }
        }
    }

    @Override
    public void disable() {
        // 停用只拆除运行时状态并保存 PDC，不删除作为存档载体的 ItemDisplay。
        // isStopping 避免在服务器收尾期间从家具卸载回调继续批量增删 Collider/座椅。
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                try {
                    if (entity instanceof ItemDisplay itemDisplay) {
                        unloadFurnitureFromEntity(itemDisplay, true);
                    } else if (CraftEngineFurniture.isCollisionEntity(entity)) {
                        unregisterColliderEntity(entity);
                        if (!VersionHelper.hasFoliaPatch) {
                            entity.remove();
                        }
                    }
                } catch (Throwable t) {
                    Debugger.FURNITURE.warn(() -> "Failed to unload entity " + entity, t);
                }
            }
        }
        super.disable();
        HandlerList.unregisterAll(this.furnitureEventListener);
        if (this.paperFurnitureEventListener != null) HandlerList.unregisterAll(this.paperFurnitureEventListener);
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

    public void handleFurnitureTeleport(ItemDisplay entity) {
        BukkitFurniture previous = this.byMetaEntityId.get(entity.getEntityId());
        if (previous == null || previous.isMoving()) return;
        // 跨世界传送可能先复制实体 NBT，再卸载旧实体；提前保存尚未写入 PDC 的行为数据
        previous.saveIfDirty();
        this.plugin.scheduler().platform().runDelayed(() -> {
            if (!entity.isValid()) return;
            // Spigot 没有 Paper 的单实体卸载/加载事件，需要在底层实体被替换后补做恢复。
            if (this.byMetaEntityId.get(previous.entityId()) == previous && previous.metaDataEntity.minecraftEntity() != CraftEntityProxy.INSTANCE.getEntity(entity)) {
                this.unloadFurniture(previous, false);
            }
            BukkitFurniture current = this.byMetaEntityId.get(entity.getEntityId());
            if (current == null) {
                this.restoreFurnitureFromEntity(entity);
            } else {
                current.synchronizePosition();
            }
        }, () -> {}, entity);
    }

    /**
     * 结束一个已登记家具的运行时生命周期，不等同于玩家拆除家具。
     * Paper 单实体移除、区块/世界卸载、插件停用都会到达这里；先撤销映射实现去重。
     * /kill 和 WorldEdit 删除走 onUnload，不会补调用 destroy(player) 的 preRemove/postRemove。
     * 不要增加 isValid() 判断：已核对版本的单实体移除事件触发时 valid 已被设为 false。
     */
    public void unloadFurnitureFromEntity(ItemDisplay entity, boolean isStopping) {
        int id = entity.getEntityId();
        BukkitFurniture furniture = this.byMetaEntityId.get(id);
        if (furniture != null) {
            this.unloadFurniture(furniture, isStopping);
        }
    }

    private void unloadFurniture(BukkitFurniture furniture, boolean isStopping) {
        // Spigot 卸载回调仍在 section 遍历中；只撤销登记，旧 Collider 交给区块加载入口清理。
        this.unregisterFurniture(furniture, !isStopping);

        // 触发行为卸载
        try {
            furniture.controller.onUnload();
        } finally {
            furniture.saveIfDirty();
        }
    }

    // 单个 Collider 离开世界只清理反向映射，不递归卸载整个家具，也不自动重建这个 Collider。
    public void unregisterColliderEntity(Entity entity) {
        int id = entity.getEntityId();
        this.byColliderEntityId.remove(id);
    }

    // CE 自己的阶段标记：批量恢复完成后置 true，已有区块启动扫描也会置 true，CEChunk.unload 清除。
    // 不是 Bukkit Chunk.isEntitiesLoaded()，也不表示 Paper 当前允许实体增删。
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isChunkEntityRestoreComplete(Location location) {
        CEWorld ceWorld = BukkitAdaptor.adapt(location.getWorld()).storageWorld();
        CEChunk ceChunk = ceWorld.getChunkAtIfLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (ceChunk == null) return false;
        return ceChunk.isEntitiesLoaded();
    }

    /**
     * 从元数据实体的 PDC 恢复家具；供区块批量加载、世界/启动扫描及放置时的 ID 迁移使用。
     * 完成 ID 迁移、运行时登记、行为数据恢复和 onLoad，再给已有观察者补发生成包。
     */
    public void restoreFurnitureFromEntity(ItemDisplay entity) {
        restoreFurnitureFromEntity(entity, null);
    }

    /** 同一 EntitiesLoadEvent 可共享 runner；不要跨事件或跨 tick 保存这个 runner。 */
    public void restoreFurnitureFromEntity(ItemDisplay entity, @Nullable SafeEntityOperationRunner runner) {
        // 实体可能不是持久的
        if (!entity.isPersistent()) {
            return;
        }

        // 获取家具pdc
        String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (id == null) return;

        // 已经在其他事件里加载过了
        BukkitFurniture previous = this.byMetaEntityId.get(entity.getEntityId());
        if (previous != null) return;

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

        // 创建新的家具
        createFurnitureAndLoadBehavior(entity, optionalFurniture.get(), readFurniturePersistentData(entity), runner);
        // 区块恢复时，追踪器可能在批量事件之前就发出了生成包；已有实体扫描也有同样的问题。
        // 必须在登记完成后补包，否则网络线程仍将元数据识别为普通 ItemDisplay，虚拟显示不会生成。
        sendSpawnPacketToTrackedPlayers(entity);
    }

    // 完整恢复入口：先构建/登记，再恢复行为数据并通知 onLoad。
    // Collider 的实际入世界操作可能延迟一 tick，因此 onLoad 不保证所有派生实体已经有效。
    private void createFurnitureAndLoadBehavior(ItemDisplay entity, FurnitureDefinition definition, FurniturePersistentData persistentData, @Nullable SafeEntityOperationRunner runner) {
        BukkitFurniture furnitureInstance = createAndRegisterFurniture(entity, definition, persistentData, runner);
        CompoundTag data = (CompoundTag) Optional.ofNullable(furnitureInstance.persistentData.getTag(FurniturePersistentData.CUSTOM_DATA)).orElseGet(CompoundTag::new);
        furnitureInstance.controller.loadCustomData(data);
        furnitureInstance.controller.onLoad();
        // 首次放置由后续生成包同步；恢复已有实体由调用方补发生成包，无需另发更新通知。
        furnitureInstance.publishClientSnapshot(List.of());
    }

    /**
     * Paper 事件和 Spigot Agent 的单实体补载入口，主要覆盖 WorldEdit/外部 NMS 生成。
     * 此事件位于 ServerLevel.EntityCallbacks.onTrackingStart 内；追踪器已建立，生成包可能已发送，
     * 但 EntityLookup 的状态切换尚未退出。因此既需要补包，又可能需要推迟 Collider 入世界。
     * 普通区块恢复时它同样触发，各版本均通过 CE 阶段标记交给后面的批量入口处理。
     */
    public void handleFurnitureEntityAdded(ItemDisplay entity) {
        // 实体可能不是持久的
        // 这里不要检查是否持久化，因为这里通常是后加的

        // 获取家具pdc
        String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (id == null) return;

        // CE 区块不存在或尚未完成批量恢复时，在这里跳过，等 EntitiesLoadEvent。
        Location location = entity.getLocation();
        if (!isChunkEntityRestoreComplete(location)) {
            return;
        }

        // 已经在其他事件里加载过了
        BukkitFurniture previous = this.byMetaEntityId.get(entity.getEntityId());
        if (previous != null) return;

        // 获取家具配置
        Key key = Key.of(id);
        Optional<FurnitureDefinition> optionalFurniture = furnitureById(key);
        if (optionalFurniture.isEmpty()) return;

        // 外部复制的实体同样需要恢复行为并发布客户端快照，否则补包也无法显示。
        createFurnitureAndLoadBehavior(entity, optionalFurniture.get(), readFurniturePersistentData(entity), null);
        sendSpawnPacketToTrackedPlayers(entity);
    }

    /**
     * 补发早于家具登记的生成包，让 AddEntityListener 接管虚拟元素/HitBox；不重新添加 NMS 实体。
     * 原包若已识别为家具，网络侧会处理重复包；尚未追踪的玩家则等待正常的初始生成包。
     */
    private void sendSpawnPacketToTrackedPlayers(ItemDisplay entity) {
        List<Player> trackedBy = EntityUtils.getTrackedByList(entity, BukkitAdaptor::adapt);
        if (trackedBy.isEmpty()) return;
        Location location = entity.getLocation();
        Object packet = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(
                entity.getEntityId(), entity.getUniqueId(), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(),
                EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0
        );
        for (Player player : trackedBy) {
            player.sendPacket(packet, false);
        }
    }

    /** 单实体事件：保留正常创建的自定义 Collider，只清理带碰撞标记的普通 NMS 实体副本。 */
    public void removeCopiedColliderEntity(Entity entity) {
        // 正常 addCollidersToWorld 同样触发 EntityAddToWorldEvent，不能在这里删除它。
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
        if (!isChunkEntityRestoreComplete(location)) {
            return;
        }

        // WorldEdit 的 NBT 复制保留 PDC 标记，但按原版类型重新创建，丢失 CollisionEntity 子类。
        // 刚进入追踪回调的副本可能处于禁止移除状态，必须经安全操作入口延后处理。
        runSafeEntityOperation(location.getChunk(), entity::remove);
    }

    /**
     * 批量恢复/扫描时清理旧 Collider；家具会从元数据实体重新生成运行时 Collider。
     * 与单实体入口不同，这里连 CollisionEntity 子类也删除，不能拿来处理正常 Collider 的 add 事件。
     */
    public void removeStaleColliderEntity(Entity collisionEntity) {
        // 原有运行时子类或携带 PDC 标记的存档/复制副本都属于本入口的清理对象。
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

    private FurniturePersistentData readFurniturePersistentData(Entity baseEntity) {
        byte[] extraData = baseEntity.getPersistentDataContainer().get(FURNITURE_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY);
        if (extraData == null) return new FurniturePersistentData(null);
        try {
            return FurniturePersistentData.fromBytes(extraData);
        } catch (IOException e) {
            // 损坏了？一般不会
            return new FurniturePersistentData(null);
        }
    }

    // 顺序不可随意调整：构造快照 -> 登记所有 ID -> 添加真实 Collider -> 激活显示元素。
    // 登记必须早于派生实体入世界，否则同步事件/生成包处理器无法识别它们所属的家具。
    private BukkitFurniture createAndRegisterFurniture(ItemDisplay display,
                                                       FurnitureDefinition furniture,
                                                       FurniturePersistentData data,
                                                       @Nullable SafeEntityOperationRunner runner) {
        BukkitFurniture bukkitFurniture = new BukkitFurniture(display, furniture, data);
        registerFurniture(bukkitFurniture);
        Location location = display.getLocation();
        Runnable action = () -> {
            bukkitFurniture.addCollidersToWorld();
            List<FurnitureElement> elements = bukkitFurniture.elements();
            for (int elementIndex = 0, elementCount = elements.size(); elementIndex < elementCount; elementIndex++) {
                FurnitureElement element = elements.get(elementIndex);
                element.activate();
            }
        };
        if (runner != null) {
            runner.run(action);
        } else {
            runSafeEntityOperation(location.getChunk(), action);
        }
        return bukkitFurniture;
    }

    /** 登记实体 ID 映射并按需安装 ticker；也用于变体切换或移动后的重新登记。 */
    void registerFurniture(BukkitFurniture furniture) {
        int entityId = furniture.entityId();
        this.byMetaEntityId.put(entityId, furniture);
        this.byInteractableEntityId.put(entityId, furniture);
        for (int id : furniture.interactableEntityIds()) {
            this.byInteractableEntityId.put(id, furniture);
        }
        List<Collider> colliders = furniture.colliders();
        for (int colliderIndex = 0, colliderCount = colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
            Collider collisionEntity = colliders.get(colliderIndex);
            this.byColliderEntityId.put(collisionEntity.entityId(), furniture);
        }
        if (!this.syncTickers.containsKey(entityId)) {
            FurnitureTicker<FurnitureController> ticker = furniture.controller.createFurnitureTicker();
            if (ticker != null) {
                TickingFurnitureImpl<FurnitureController> tickingFurniture = new TickingFurnitureImpl<>(furniture, ticker);
                this.syncTickers.put(entityId, tickingFurniture);
                if (VersionHelper.hasFoliaPatch) {
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

    /** 撤销实体 ID 映射、停用元素，并按需移除碰撞实体和座椅；不调用行为 onUnload。 */
    void unregisterFurniture(BukkitFurniture furniture, boolean removeSubEntities) {
        int entityId = furniture.entityId();
        // 移除entity id映射
        this.byMetaEntityId.remove(entityId);
        this.byInteractableEntityId.remove(entityId);
        for (int id : furniture.interactableEntityIds()) {
            this.byInteractableEntityId.remove(id);
        }
        List<Collider> colliders = furniture.colliders();
        for (int colliderIndex = 0, colliderCount = colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
            Collider collisionEntity = colliders.get(colliderIndex);
            if (removeSubEntities) {
                Object entity = collisionEntity.handle();
                if (EntityProxy.INSTANCE.isRemoved(entity)) continue;
                if (VersionHelper.hasPaperPatch) {
                    Object level = EntityProxy.INSTANCE.getLevel(entity);
                    Object entityLookup = LevelUtils.getEntityLookup(level);
                    if (!EntityLookupProxy.INSTANCE.canRemoveEntity(entityLookup, entity)) return;
                }
                collisionEntity.destroy();
            }
            this.byColliderEntityId.remove(collisionEntity.entityId());
        }
        if (removeSubEntities) {
            for (FurnitureHitBox hitbox : furniture.hitboxes()) {
                for (Seat<?> seat : hitbox.seats()) {
                    Entity seatEntity = ((BukkitSeat<?>) seat).getSeatEntity();
                    if (seatEntity == null || !seatEntity.isValid()) continue;
                    if (VersionHelper.hasPaperPatch) {
                        Object entity = CraftEntityProxy.INSTANCE.getEntity(seatEntity);
                        Object level = EntityProxy.INSTANCE.getLevel(entity);
                        Object entityLookup = LevelUtils.getEntityLookup(level);
                        if (!EntityLookupProxy.INSTANCE.canRemoveEntity(entityLookup, entity)) continue;
                    }
                    seat.destroy();
                }
            }
        }
        List<FurnitureElement> elements = furniture.elements();
        for (int elementIndex = 0, elementCount = elements.size(); elementIndex < elementCount; elementIndex++) {
            FurnitureElement element = elements.get(elementIndex);
            element.deactivate();
        }
    }

    // 单实体/section 的状态切换期间 Paper 会拒绝移除。这里跳过，不创建重试任务。
    // 普通区块卸载随后还有批量清理；不要把它当作任意时机都保证成功的 destroy。
    private void tryRemoveCollider(Collider collider) {

    }

    // 1.20.x 使用 io.papermc.paper 的 EntityLookup；本地 1.21.1+ 样本使用 Moonrise 同等机制。
    private boolean shouldDeferEntityOperation(Chunk chunk) {
        if (!VersionHelper.hasPaperPatch) return false;
        Object world = CraftWorldProxy.INSTANCE.getWorld(chunk.getWorld());
        Object entityLookup = LevelUtils.getEntityLookup(world);
        Object slices = EntityLookupProxy.INSTANCE.getChunk(entityLookup, chunk.getX(), chunk.getZ());
        return slices != null && ChunkEntitySlicesProxy.INSTANCE.isPreventingStatusUpdates(slices);
    }

    // 延迟一 tick 用来退出 Paper 状态切换栈；在 Folia 上 scheduler 按目标区块调度。
    // 它不会把任意异步调用变成合法操作，也不会在执行时复核家具是否仍然登记。
    private void runSafeEntityOperation(Chunk chunk, Runnable action) {
        if (!chunk.isLoaded()) return;
        if (shouldDeferEntityOperation(chunk)) {
            this.plugin.scheduler().platform().runLater(action, 1, chunk.getWorld(), chunk.getX(), chunk.getZ());
        } else {
            action.run();
        }
    }

    public SafeEntityOperationRunner newEntityOperationRunner(Chunk chunk) {
        return new SafeEntityOperationRunner(chunk);
    }

    /**
     * 只供同一个区块的一次同步批量处理使用，共享 shouldDeferEntityOperation 的结果。
     * 每个 action 仍分别提交任务；这不是合并任务队列，也不是长期有效的区块状态缓存。
     */
    public final class SafeEntityOperationRunner {
        private final Chunk chunk;
        private boolean resolved;
        private boolean defer;

        private SafeEntityOperationRunner(Chunk chunk) {
            this.chunk = chunk;
        }

        public void run(Runnable action) {
            if (!this.chunk.isLoaded()) return;
            if (!this.resolved) {
                this.defer = shouldDeferEntityOperation(this.chunk);
                this.resolved = true;
            }
            if (this.defer) {
                BukkitFurnitureManager.this.plugin.scheduler().platform().runLater(action, 1, this.chunk.getWorld(), this.chunk.getX(), this.chunk.getZ());
            } else {
                action.run();
            }
        }
    }

    @Override
    protected FurnitureHitBoxConfig<?> defaultHitBox() {
        return InteractionFurnitureHitboxConfig.DEFAULT;
    }
}
