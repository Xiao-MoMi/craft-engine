package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.element.*;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ChainParameterSource;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.parameter.FurnitureParameterProvider;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public abstract class Furniture implements Cullable, ChainParameterSource {
    public final FurnitureDefinition config;
    /**
     * Accessor for persistent furniture data
     */
    public final FurniturePersistentData persistentData;
    /**
     * The base entity that carries metadata for this furniture
     */
    public final Entity metaDataEntity;
    /**
     * Cached entity ID of the metadata entity
     */
    public final int metaDataEntityId;
    /**
     * Furniture controller
     */
    public final FurnitureController controller;

    protected CullingData cullingData;
    // Published by the entity thread and read by network handlers.
    protected volatile FurnitureSnapshotState snapshot;
    protected FurnitureVariant currentVariant;
    protected Item sourceItem;
    /**
     * IDs of virtual entities that need to be sent to clients
     */
    protected int[] interactableEntityIds;
    /**
     * IDs of entities specifically acting as physics colliders
     */
    protected int[] colliderEntityIds;
    protected volatile boolean unsaved;
    private boolean hasExternalModel;
    private volatile FurniturePlacement placement;

    protected Furniture(Entity metaDataEntity, FurniturePersistentData data, FurnitureDefinition config) {
        this.config = config;
        this.persistentData = data;
        this.metaDataEntity = metaDataEntity;
        this.metaDataEntityId = metaDataEntity.entityId();
        this.updatePlacement();
        this.sourceItem = data.item().orElse(null);
        this.controller = FurnitureController.createController(this);
        this.setVariantInternal(config.getVariant(data));
    }

    @Override
    public <T> Optional<T> getParameter(ContextKey<T> key) {
        return FurnitureParameterProvider.INSTANCE.getOptionalParameter(key, this);
    }

    public WorldPosition position() {
        return this.placement.origin;
    }

    public FurniturePlacement placement() {
        return this.placement;
    }

    protected void updatePlacement() {
        this.placement = new FurniturePlacement(this.metaDataEntity.position());
    }

    public World world() {
        return this.metaDataEntity.world();
    }

    public int entityId() {
        return this.metaDataEntityId;
    }

    public Entity metaDataEntity() {
        return this.metaDataEntity;
    }

    /**
     * Gets the source item instance
     * Affects the drops when this furniture is broken, as well as the furniture's dyed color
     * <p>
     * When placing via the API, this value may be empty. If you need to retrieve the items corresponding to a piece of furniture, please call {@link #buildNewFurnitureItem()}.
     */
    @Nullable
    public Item sourceItem() {
        return this.sourceItem;
    }

    /**
     * Sets the source item instance
     * Affects the drops when this furniture is broken, as well as the furniture's dyed color
     * <p>
     * Note: If you need to change the furniture's color, you must call {@link #refreshElements()}
     * after setting the sourceItem's color to refresh the display effect
     *
     * @param sourceItem The new source item instance
     */
    public void setSourceItem(@Nullable Item sourceItem) {
        this.sourceItem = sourceItem;
        this.persistentData.setItem(sourceItem);
    }

    /**
     * Gets the snapshot state
     *
     * @return snapshot state
     */
    @ApiStatus.Internal
    public FurnitureSnapshotState snapshotState() {
        return this.snapshot;
    }

    /**
     * Build the item corresponding to this piece of furniture. If there are no corresponding item or the item does not exist, return null.
     *
     * @return the furniture item
     */
    @Nullable
    public Item buildNewFurnitureItem() {
        Key itemId = this.config.settings().itemId();
        if (itemId == null) {
            return null;
        }
        return Item.byId(itemId);
    }

    /**
     * Checks whether this furniture is currently using an external model engine.
     * <p>
     * When true, the furniture's visual representation is handled by an external
     * plugin (e.g., ModelEngine or BetterModel) rather than standard furniture elements.
     * </p>
     *
     * @return {@code true} if an external model is bound to this furniture instance.
     */
    public boolean hasExternalModel() {
        return this.hasExternalModel;
    }

    /**
     * Gets the active variant definition for this furniture.
     * The variant determines the specific model, hitboxes, and properties
     * currently being used
     *
     * @return The current {@link FurnitureVariant}.
     */
    public FurnitureVariant currentVariant() {
        return this.currentVariant;
    }

    /**
     * Alias for {@link #currentVariant()}.
     *
     * @return The current {@link FurnitureVariant}.
     */
    public FurnitureVariant getCurrentVariant() {
        return this.currentVariant;
    }

    /**
     * Changes the variant of the furniture.
     * <p>
     * This implementation performs a safety check to ensure the new variant's hitboxes
     * do not collide with existing world entities before proceeding with the swap.
     * </p>
     *
     * @param variantName The name of the variant to switch to.
     * @return true if successful.
     */
    public boolean setVariant(String variantName) {
        return this.setVariant(variantName, false);
    }

    /**
     * Changes the variant of the furniture.
     * <p>
     * This implementation performs a safety check to ensure the new variant's hitboxes
     * do not collide with existing world entities before proceeding with the swap.
     * </p>
     *
     * @param variantName The key of the variant to switch to.
     * @param force       If true, skips the collision check and forces the transition.
     * @return {@code true} if the variant was successfully changed.
     */
    public abstract boolean setVariant(String variantName, boolean force);

    /**
     * Refreshes the visual elements for all tracking players.
     */
    public void refreshElements() {
        List<Player> trackedBy = trackedBy();
        for (int playerIndex = 0, playerCount = trackedBy.size(); playerIndex < playerCount; playerIndex++) {
            Player player = trackedBy.get(playerIndex);
            refreshElements(player);
        }
    }

    /**
     * Refreshes visual elements for a specific player.
     */
    public void refreshElements(Player player) {
        this.snapshot.refreshElements(player);
    }

    /**
     * Moves the furniture to a new position.
     *
     * @param position New world position.
     * @return A future containing the result of the move.
     */
    public CompletableFuture<Boolean> moveTo(WorldPosition position) {
        return this.moveTo(position, false);
    }

    /**
     * Moves the furniture to a new position.
     *
     * @param position New world position.
     * @param force    Whether to force the move even if obstructed.
     * @return A future containing the result of the move.
     */
    public abstract CompletableFuture<Boolean> moveTo(WorldPosition position, boolean force);

    /**
     * Triggers a full refresh (elements & hitboxes) for all tracking players.
     */
    public void refresh() {
        List<Player> trackedBy = trackedBy();
        for (int playerIndex = 0, playerCount = trackedBy.size(); playerIndex < playerCount; playerIndex++) {
            Player player = trackedBy.get(playerIndex);
            refresh(player);
        }
    }

    /**
     * Triggers a full refresh (elements & hitboxes) for player
     */
    public abstract void refresh(Player player);

    /**
     * Destroys and removes all active colliders.
     */
    protected void clearColliders() {
        this.snapshot.clearColliders();
    }

    /**
     * 构建服务端变体快照：准备元素、交互箱、座椅、物理 Collider 和剔除数据。
     * 此处只复用实体 ID、构造新状态，不发送客户端变体更新包。
     * 平台层完成行为回调和实体登记后再发布，由玩家网络线程比较已应用快照。
     */
    protected void setVariantInternal(FurnitureVariant variant) {
        FurnitureVariant previousVariant = this.currentVariant;
        buildVariantSnapshot(variant);

        // 外部模型
        Supplier<ExternalModel> externalModel = variant.externalModel();
        if (externalModel != null) {
            Optional.ofNullable(externalModel.get()).ifPresent(model -> {
                this.hasExternalModel = true;
                try {
                    model.bindModel(this.metaDataEntity);
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("Failed to load external model for furniture " + id(), e);
                }
            });
        } else {
            this.hasExternalModel = false;
        }

        // 触发变体变化，可能变体也无变化，而只是移动
        if (previousVariant != null) {
            this.controller.onVariantChange(previousVariant);
        }
    }

    private void buildVariantSnapshot(FurnitureVariant variant) {
        this.currentVariant = variant;
        this.persistentData.setVariant(variant.name());

        // 所有可供交互的实体列表
        IntList interactableEntityIds = new IntArrayList(variant.elementConfigs().size() + variant.hitBoxConfigs().size());
        IntConsumer interactableCollector = interactableEntityIds::add;

        // 获取全部家具显示元素，从行为和配置里获取
        List<FurnitureElementConfig<? extends FurnitureElement>> elementConfigs = variant.elementConfigs();
        List<FurnitureElement> elements;

        // 如果先前存在变体快照
        if (this.snapshot != null) {
            elements = this.updateElements(elementConfigs);
            for (int elementIndex = 0, elementCount = elements.size(); elementIndex < elementCount; elementIndex++) {
                FurnitureElement element = elements.get(elementIndex);
                element.gatherInteractableEntityId(interactableCollector);
            }
        } else {
            elements = new ArrayList<>(elementConfigs.size());
            for (int configIndex = 0, configCount = elementConfigs.size(); configIndex < configCount; configIndex++) {
                FurnitureElementConfig<?> elementConfig = elementConfigs.get(configIndex);
                FurnitureElement element = elementConfig.create(this);
                elements.add(element);
                element.gatherInteractableEntityId(interactableCollector);
            }
        }

        // 行为提供的元素
        this.controller.gatherElements(element -> {
            elements.add(element);
            element.gatherInteractableEntityId(interactableCollector);
        });

        // 初始化碰撞箱
        List<FurnitureHitBoxConfig<? extends FurnitureHitBox>> furnitureHitBoxConfigs = variant.hitBoxConfigs();
        ObjectArrayList<ColliderConfig> colliderConfigs = new ObjectArrayList<>(furnitureHitBoxConfigs.size());
        List<FurnitureHitBox> hitboxes = new ObjectArrayList<>(furnitureHitBoxConfigs.size());

        // 辅助map，用于排除重复的座椅
        Map<Vector3f, Seat<SeatOwner>> seatMap = null;
        for (int configIndex = 0, configCount = furnitureHitBoxConfigs.size(); configIndex < configCount; configIndex++) {
            FurnitureHitBoxConfig<?> furnitureHitBoxConfig = furnitureHitBoxConfigs.get(configIndex);
            FurnitureHitBox hitbox = furnitureHitBoxConfig.create(this);
            hitboxes.add(hitbox);
        }
        this.controller.gatherHitboxes(hitboxes::add);

        int partCount = 0;
        for (int i = 0; i < hitboxes.size(); i++) {
            partCount += hitboxes.get(i).partCount();
        }
        Int2ObjectMap<FurnitureHitBox> hitboxMap = new Int2ObjectOpenHashMap<>(partCount);
        ColliderMergePlan mergePlan = variant.colliderMergePlan();
        int[] configuredColliders = mergePlan.hasMerges ? new int[furnitureHitBoxConfigs.size()] : IntArrays.EMPTY_ARRAY;
        if (mergePlan.hasMerges) Arrays.fill(configuredColliders, -1);
        for (int hitboxIndex = 0, size = hitboxes.size(); hitboxIndex < size; hitboxIndex++) {
            FurnitureHitBox hitbox = hitboxes.get(hitboxIndex);
            for (int i = 0, count = hitbox.partCount(); i < count; i++) {
                FurnitureHitboxPart part = hitbox.part(i);
                hitboxMap.put(part.entityId(), hitbox);
            }
            Seat<SeatOwner>[] seats = hitbox.seats();
            for (int index = 0; index < seats.length; index++) {
                if (seatMap == null) seatMap = new HashMap<>(4);
                Vector3f seatPos = seats[index].config().position();
                Seat<SeatOwner> existing = seatMap.putIfAbsent(seatPos, seats[index]);
                if (existing != null) seats[index] = existing;
            }
            hitbox.collectInteractableEntityId(interactableCollector);
            int colliderCount = hitbox.colliderConfigCount();
            if (hitboxIndex < configuredColliders.length && colliderCount == 1) {
                configuredColliders[hitboxIndex] = colliderConfigs.size();
            }
            for (int i = 0; i < colliderCount; i++) {
                colliderConfigs.add(hitbox.colliderConfig(i));
            }
        }

        mergePlan.optimize(colliderConfigs, configuredColliders);
        List<Collider> colliders = new ObjectArrayList<>(colliderConfigs.size());
        this.colliderEntityIds = new int[colliderConfigs.size()];
        for (int i = 0; i < colliderConfigs.size(); i++) {
            Collider collider = createCollider(colliderConfigs.get(i));
            colliders.add(collider);
            this.colliderEntityIds[i] = collider.entityId();
        }

        // 虚拟碰撞箱的实体id
        this.interactableEntityIds = interactableEntityIds.toIntArray();
        this.cullingData = createCullingData(variant.cullingData(), hitboxes);
        this.snapshot = createSnapshot(elements, hitboxes, hitboxMap, colliders);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<FurnitureElement> updateElements(List<FurnitureElementConfig<? extends FurnitureElement>> newElementConfigList) {
        List<FurnitureElement> newElements = new ArrayList<>(newElementConfigList.size());
        if (this.snapshot.elements.isEmpty() && newElementConfigList.isEmpty()) return newElements;
        /*
         *
         * 1 对 1，命中率最高
         *
         */
        if (this.snapshot.elements.size() == 1 && newElementConfigList.size() == 1) {
            FurnitureElement previousElement = this.snapshot.elements.getFirst();
            FurnitureElementConfig<? extends FurnitureElement> config = newElementConfigList.getFirst();
            FurnitureElement element = null;
            WorldPosition position = null;
            if (config instanceof TransformableFurnitureElementConfig<?> transformConfig) {
                position = transformConfig.getPos(this);
                if (previousElement instanceof TransformableFurnitureElement transformable && transformConfig.elementClass().isInstance(previousElement)) {
                    element = ((TransformableFurnitureElementConfig) transformConfig).transform(this, transformable,
                            position);
                }
            }
            if (element == null) {
                element = config instanceof TransformableFurnitureElementConfig<?> transformConfig ? transformConfig.create(this, position) : config.create(this);
            }
            newElements.add(element);
        } else {
            FurnitureElementMatcher matcher = new FurnitureElementMatcher(this.snapshot.elements);
            FurnitureElementConfig<?>[] unmatched = null;
            WorldPosition[] unmatchedPositions = null;
            int unmatchedCount = 0;
            int configCount = newElementConfigList.size();
            for (int configIndex = 0; configIndex < configCount; configIndex++) {
                FurnitureElementConfig<?> config = newElementConfigList.get(configIndex);
                WorldPosition position = null;
                TransformableFurnitureElement previousElement = null;
                FurnitureElement element = null;
                if (config instanceof TransformableFurnitureElementConfig<?> transformConfig) {
                    position = transformConfig.getPos(this);
                    previousElement = matcher.match(transformConfig.elementClass(), position, true);
                    if (previousElement != null) {
                        element = ((TransformableFurnitureElementConfig) transformConfig).transform(this, previousElement, position);
                    }
                }
                if (element == null) {
                    if (unmatched == null) unmatched = new FurnitureElementConfig<?>[configCount];
                    unmatched[unmatchedCount] = config;
                    if (position != null) {
                        if (unmatchedPositions == null) unmatchedPositions = new WorldPosition[configCount];
                        unmatchedPositions[unmatchedCount] = position;
                    }
                    unmatchedCount++;
                } else {
                    newElements.add(element);
                }
            }
            for (int i = 0; i < unmatchedCount; i++) {
                FurnitureElementConfig<?> config = unmatched[i];
                TransformableFurnitureElement previousElement;
                FurnitureElement element;
                if (config instanceof TransformableFurnitureElementConfig<?> transformConfig) {
                    // The first pass stores a non-null position for every unmatched transformable config.
                    assert unmatchedPositions != null;
                    WorldPosition position = unmatchedPositions[i];
                    assert position != null;
                    previousElement = matcher.match(transformConfig.elementClass(), position, false);
                    element = previousElement != null
                            ? ((TransformableFurnitureElementConfig) transformConfig).transform(this, previousElement, position)
                            : transformConfig.create(this, position);
                } else {
                    element = config.create(this);
                }
                newElements.add(element);
            }
        }
        return newElements;
    }

    protected abstract FurnitureSnapshotState createSnapshot(List<FurnitureElement> elements,
                                                             List<FurnitureHitBox> hitboxes,
                                                             Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                                             List<Collider> colliders);

    /**
     * Creates culling data based on hitboxes or pre-defined AABB.
     * Takes furniture rotation into account.
     */
    private CullingData createCullingData(CullingData parent, List<FurnitureHitBox> hitboxes) {
        if (parent == null) return null;
        AABB aabb = parent.aabb;
        WorldPosition position = position();
        if (aabb == null) {
            CullingBounds bounds = new CullingBounds();
            for (int i = 0; i < hitboxes.size(); i++) {
                hitboxes.get(i).collectCullingBounds(bounds);
            }
            return new CullingData(bounds.toAABB(position), parent.maxDistance, parent.aabbExpansion, parent.rayTracing);
        } else {
            Vector3f[] vertices = new Vector3f[]{
                    // 底面两个对角点
                    new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ),
                    new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ),
                    // 顶面两个对角点
                    new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ),
                    new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ)
            };
            double minX = Double.MAX_VALUE, minY = aabb.minY; // Y方向不变
            double maxX = -Double.MAX_VALUE, maxY = aabb.maxY; // Y方向不变
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            for (Vector3f vertex : vertices) {
                Vec3d rotatedPos = getRelativePosition(vertex);
                minX = Math.min(minX, rotatedPos.x);
                minZ = Math.min(minZ, rotatedPos.z);
                maxX = Math.max(maxX, rotatedPos.x);
                maxZ = Math.max(maxZ, rotatedPos.z);
            }
            return new CullingData(new AABB(minX, minY, minZ, maxX, maxY, maxZ),
                    parent.maxDistance, parent.aabbExpansion, parent.rayTracing);
        }
    }

    @Nullable
    public FurnitureHitBox hitboxByEntityId(int entityId) {
        return this.snapshot.hitboxByEntityId(entityId);
    }

    @Nullable
    @Override
    public CullingData cullingData() {
        return this.cullingData;
    }

    public Key id() {
        return this.config.id();
    }

    public int[] interactableEntityIds() {
        return this.interactableEntityIds;
    }

    public int[] colliderEntityIds() {
        return colliderEntityIds;
    }

    public UUID uuid() {
        return this.metaDataEntity.uuid();
    }

    @Override
    public void show(Player player) {
        this.snapshot.show(player);
    }

    @Override
    public void hide(Player player) {
        this.snapshot.hide(player);
    }

    public void addCollidersToWorld() {
        this.snapshot.addCollidersToWorld(this.world());
    }

    /**
     * Destroys all seats associated with this furniture.
     */
    public void destroySeats() {
        this.snapshot.destroySeats();
    }

    public boolean isValid() {
        return this.metaDataEntity.isValid();
    }

    /**
     * Fully removes the furniture from the world and cleans up resources.
     */
    public abstract void destroy(Player player);

    public void destroy() {
        destroy(null);
    }

    /**
     * Gets the configuration of this furniture.
     *
     * @return The {@link FurnitureDefinition} configuration.
     */
    public FurnitureDefinition config() {
        return this.config;
    }

    /**
     * Alias for {@link #config()}.
     *
     * @return The {@link FurnitureDefinition} configuration.
     */
    public FurnitureDefinition furniture() {
        return this.config;
    }

    /**
     * Gets the persistent data container for this specific furniture instance.
     *
     * @return The {@link FurniturePersistentData} for this instance.
     */
    public FurniturePersistentData persistentData() {
        return this.persistentData;
    }

    /**
     * Converts a local offset to a global world coordinate based on current furniture position and rotation.
     */
    public Vec3d getRelativePosition(Vector3f position) {
        return this.placement.relativePosition(position);
    }

    /**
     * Called only after all hitboxes have supplied their data and optimization has finished.
     */
    protected abstract Collider createCollider(ColliderConfig config);

    /**
     * Gets the collection of physical colliders associated with this furniture.
     * <p>
     * Colliders are the invisible physical boundaries used by the server's
     * physics engine to handle movement obstruction, projectile impacts,
     * and player collision.
     * </p>
     */
    public List<Collider> colliders() {
        return this.snapshot.colliders();
    }

    /**
     * Retrieves all visual elements associated with this furniture.
     * These elements handle the model rendering, animations, and client-side displays.
     * * @return An array of {@link FurnitureElement} currently active for this furniture instance.
     */
    public List<FurnitureElement> elements() {
        return this.snapshot.elements();
    }

    /**
     * Retrieves all functional hitboxes associated with this furniture.
     * * @return An array of {@link FurnitureHitBox} defining the physical interaction bounds.
     */
    public List<FurnitureHitBox> hitboxes() {
        return this.snapshot.hitboxes();
    }

    /**
     * Gets the set of players who are currently "tracking" this furniture.
     */
    public abstract Set<Player> getTrackedBy();

    /**
     * Gets the list of players who are currently "tracking" this furniture.
     */
    public abstract List<Player> trackedBy();

    /**
     * Save the custom data if it's dirty
     */
    public abstract void saveIfDirty();

    public void setUnsaved() {
        this.unsaved = true;
    }

    public boolean isUnsaved() {
        return this.unsaved;
    }

    public boolean canInteract(Player player) {
        WorldPosition position = position();
        if (!player.world().uuid().equals(position.world.uuid())) {
            return false;
        }
        if (!player.canInteractPoint(new Vec3d(position.x, position.y, position.z), 16d)) {
            return false;
        }
        return true;
    }


    private static final class CullingBounds implements Consumer<AABB> {
        private double minX, minY, minZ;
        private double maxX, maxY, maxZ;
        private boolean empty = true;

        @Override
        public void accept(AABB aabb) {
            if (this.empty) {
                minX = aabb.minX;
                minY = aabb.minY;
                minZ = aabb.minZ;
                maxX = aabb.maxX;
                maxY = aabb.maxY;
                maxZ = aabb.maxZ;
                this.empty = false;
            } else {
                minX = Math.min(minX, aabb.minX);
                minY = Math.min(minY, aabb.minY);
                minZ = Math.min(minZ, aabb.minZ);
                maxX = Math.max(maxX, aabb.maxX);
                maxY = Math.max(maxY, aabb.maxY);
                maxZ = Math.max(maxZ, aabb.maxZ);
            }
        }

        private AABB toAABB(WorldPosition pos) {
            return this.empty ? new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z)
                    : new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    /**
     * Static utility to calculate relative coordinates based on rotation.
     */
    public static Vec3d getRelativePosition(WorldPosition location, Vector3f position) {
        Vector3f offset = rotateHitboxOffset(location.yRot(), position);
        return new Vec3d(location.x + offset.x, location.y + offset.y, location.z - offset.z);
    }

    public static Vector3f rotateHitboxOffset(float yaw, Vector3f position) {
        // Exact quarter turns keep adjoining faces aligned; quaternion roundoff can open tiny gaps.
        if (yaw % 90 == 0) {
            float normalized = (yaw % 360 + 360) % 360;
            if (normalized == 180) return new Vector3f(position);
            if (normalized == 0) return new Vector3f(-position.x, position.y, -position.z);
            if (normalized == 90) return new Vector3f(-position.z, position.y, position.x);
            if (normalized == 270) return new Vector3f(position.z, position.y, -position.x);
        }
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0f, (float) Math.toRadians(180 - yaw), 0f).conjugate();
        return conjugated.transform(new Vector3f(position));
    }
}
