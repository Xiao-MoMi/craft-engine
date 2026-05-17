package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.CustomDataType;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class Furniture implements Cullable {
    public final FurnitureDefinition config;
    /** Accessor for persistent furniture data */
    public final FurniturePersistentData persistentData;
    /** The base entity that carries metadata for this furniture */
    public final Entity metaDataEntity;
    /** Cached entity ID of the metadata entity */
    public final int metaDataEntityId;
    /** Furniture controller */
    public final FurnitureController controller;

    protected CullingData cullingData;
    protected FurnitureSnapshotState snapshot;
    protected FurnitureVariant currentVariant;
    protected Item sourceItem;
    /** IDs of virtual entities that need to be sent to clients */
    protected int[] interactableEntityIds;
    /** IDs of entities specifically acting as physics colliders */
    protected int[] colliderEntityIds;
    private boolean hasExternalModel;
    protected volatile boolean unsaved;

    protected Furniture(Entity metaDataEntity, FurniturePersistentData data, FurnitureDefinition config) {
        this.config = config;
        this.persistentData = data;
        this.metaDataEntity = metaDataEntity;
        this.metaDataEntityId = metaDataEntity.entityId();
        this.controller = FurnitureController.createController(this);
        this.setVariantInternal(config.getVariant(data));
        this.sourceItem = data.item().orElse(null);
    }

    public WorldPosition position() {
        return this.metaDataEntity.position();
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
        return CraftEngine.instance().itemManager().createWrappedItem(itemId, null);
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
        for (Player player : getTrackedBy()) {
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
     * @param position New world position.
     * @return A future containing the result of the move.
     */
    public CompletableFuture<Boolean> moveTo(WorldPosition position) {
        return this.moveTo(position, false);
    }

    /**
     * Moves the furniture to a new position.
     * @param position New world position.
     * @param force Whether to force the move even if obstructed.
     * @return A future containing the result of the move.
     */
    public abstract CompletableFuture<Boolean> moveTo(WorldPosition position, boolean force);

    /**
     * Triggers a full refresh (elements & hitboxes) for all tracking players.
     */
    public void refresh() {
        for (Player player : getTrackedBy()) {
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
     * Internal logic to initialize components based on a specific variant.
     * This sets up elements, hitboxes, seats, and culling data.
     */
    protected void setVariantInternal(FurnitureVariant variant) {
        FurnitureVariant previousVariant = this.currentVariant;
        this.currentVariant = variant;
        Int2ObjectMap<FurnitureHitBox> hitboxMap = new Int2ObjectOpenHashMap<>();

        // 所有可供交互的实体列表
        IntList interactableEntityIds = new IntArrayList();

        // 获取全部家具显示元素，从行为和配置里获取
        List<FurnitureElementConfig<? extends FurnitureElement>> elementConfigs = variant.elementConfigs();
        List<FurnitureElement> elements = new ArrayList<>(elementConfigs.size());
        for (FurnitureElementConfig<?> elementConfig : elementConfigs) {
            FurnitureElement element = elementConfig.create(this);
            elements.add(element);
            element.gatherInteractableEntityId(interactableEntityIds::addLast);
        }
        this.controller.gatherElements(element -> {
            elements.add(element);
            element.gatherInteractableEntityId(interactableEntityIds::addLast);
        });

        // 初始化碰撞箱
        List<FurnitureHitBoxConfig<? extends FurnitureHitBox>> furnitureHitBoxConfigs = variant.hitBoxConfigs();
        List<Collider> colliders = new ObjectArrayList<>(furnitureHitBoxConfigs.size());
        List<FurnitureHitBox> hitboxes = new ArrayList<>(furnitureHitBoxConfigs.size());

        // 辅助map，用于排除重复的座椅
        LazyReference<Map<Vector3f, Seat<FurnitureHitBox>>> seatMap = LazyReference.lazyReference(HashMap::new);
        for (FurnitureHitBoxConfig<?> furnitureHitBoxConfig : furnitureHitBoxConfigs) {
            FurnitureHitBox hitbox = furnitureHitBoxConfig.create(this);
            hitboxes.add(hitbox);
        }
        this.controller.gatherHitboxes(hitboxes::add);

        for (FurnitureHitBox hitbox : hitboxes) {
            for (FurnitureHitboxPart part : hitbox.parts()) {
                hitboxMap.put(part.entityId(), hitbox);
            }
            Seat<FurnitureHitBox>[] seats = hitbox.seats();
            for (int index = 0; index < seats.length; index++) {
                Map<Vector3f, Seat<FurnitureHitBox>> tempMap = seatMap.get();
                Vector3f seatPos = seats[index].config().position();
                if (tempMap.containsKey(seatPos)) {
                    seats[index] = tempMap.get(seatPos);
                } else {
                    tempMap.put(seatPos, seats[index]);
                }
            }
            hitbox.collectInteractableEntityId(interactableEntityIds::addLast);
            colliders.addAll(hitbox.colliders());
        }

        // 虚拟碰撞箱的实体id
        this.interactableEntityIds = interactableEntityIds.toIntArray();
        this.colliderEntityIds = colliders.stream().mapToInt(Collider::entityId).toArray();
        this.snapshot = createSnapshot(elements, hitboxes, hitboxMap, colliders, new IdentityHashMap<>(4));
        this.cullingData = createCullingData(variant.cullingData());

        // 外部模型
        Supplier<ExternalModel> externalModel = variant.externalModel();
        if (externalModel != null) {
            Optional.ofNullable(externalModel.get()).ifPresent(model -> {
                this.hasExternalModel = true;
                try {
                    model.bindModel((AbstractEntity) this.metaDataEntity);
                } catch (Throwable e) {
                    CraftEngine.instance().logger().warn("Failed to load external model for furniture " + id(), e);
                }
            });
        } else {
            this.hasExternalModel = false;
        }

        // 触发变体变化
        if (previousVariant != null) {
            this.controller.onVariantChange(previousVariant);
        }
    }

    protected abstract FurnitureSnapshotState createSnapshot(List<FurnitureElement> elements,
                                                             List<FurnitureHitBox> hitboxes,
                                                             Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                                             List<Collider> colliders,
                                                             Map<CustomDataType<?>, Object> customData);

    /**
     * Creates culling data based on hitboxes or pre-defined AABB.
     * Takes furniture rotation into account.
     */
    private CullingData createCullingData(CullingData parent) {
        if (parent == null) return null;
        AABB aabb = parent.aabb;
        WorldPosition position = position();
        if (aabb == null) {
            List<AABB> aabbs = new ArrayList<>();
            for (FurnitureHitBoxConfig<?> hitBoxConfig : this.currentVariant.hitBoxConfigs()) {
                hitBoxConfig.prepareBoundingBox(position, aabbs::add, true);
            }
            return new CullingData(getMaxAABB(this.position(), aabbs), parent.maxDistance, parent.aabbExpansion, parent.rayTracing);
        } else {
            Vector3f[] vertices = new Vector3f[] {
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
                Vec3d rotatedPos = getRelativePosition(position, vertex);
                minX = Math.min(minX, rotatedPos.x);
                minZ = Math.min(minZ, rotatedPos.z);
                maxX = Math.max(maxX, rotatedPos.x);
                maxZ = Math.max(maxZ, rotatedPos.z);
            }
            return new CullingData(new AABB(minX, minY, minZ, maxX, maxY, maxZ),
                    parent.maxDistance, parent.aabbExpansion, parent.rayTracing);
        }
    }

    /**
     * Calculates an enclosing AABB that contains all provided AABBs.
     */
    private static @NotNull AABB getMaxAABB(WorldPosition pos, List<AABB> aabbs) {
        double minX = pos.x;
        double minY = pos.y;
        double minZ = pos.z;
        double maxX = pos.x;
        double maxY = pos.y;
        double maxZ = pos.z;
        for (int i = 0; i < aabbs.size(); i++) {
            AABB aabb = aabbs.get(i);
            if (i == 0) {
                minX = aabb.minX;
                minY = aabb.minY;
                minZ = aabb.minZ;
                maxX = aabb.maxX;
                maxY = aabb.maxY;
                maxZ = aabb.maxZ;
            } else {
                minX = Math.min(minX, aabb.minX);
                minY = Math.min(minY, aabb.minY);
                minZ = Math.min(minZ, aabb.minZ);
                maxX = Math.max(maxX, aabb.maxX);
                maxY = Math.max(maxY, aabb.maxY);
                maxZ = Math.max(maxZ, aabb.maxZ);
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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

    /** Fully removes the furniture from the world and cleans up resources. */
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
        return getRelativePosition(this.position(), position);
    }

    /**
     * Static utility to calculate relative coordinates based on rotation.
     */
    public static Vec3d getRelativePosition(WorldPosition location, Vector3f position) {
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0f, (float) Math.toRadians(180 - location.yRot()), 0f).conjugate();
        Vector3f offset = conjugated.transform(new Vector3f(position));
        return new Vec3d(location.x + offset.x, location.y + offset.y, location.z - offset.z);
    }

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
        if (!player.canInteractPoint(new Vec3d(position.x, position.y, position.z), 16d)) {
            return false;
        }
        return true;
    }
}
