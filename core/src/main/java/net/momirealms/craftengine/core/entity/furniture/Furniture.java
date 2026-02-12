package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class Furniture implements Cullable {
    public final CustomFurniture config;
    /** Accessor for persistent furniture data */
    public final FurnitureDataAccessor dataAccessor;
    /** The base entity that carries metadata for this furniture */
    public final Entity metaDataEntity;
    /** Cached entity ID of the metadata entity */
    public final int metaDataEntityId;

    protected CullingData cullingData;
    protected FurnitureVariant currentVariant;
    protected FurnitureElement[] elements;
    protected Collider[] colliders;
    protected FurnitureHitBox[] hitboxes;
    protected Int2ObjectMap<FurnitureHitBox> hitboxMap;
    /** IDs of virtual entities that need to be sent to clients */
    protected int[] virtualEntityIds;
    /** IDs of entities specifically acting as physics colliders */
    protected int[] colliderEntityIds;
    private boolean hasExternalModel;

    protected Furniture(Entity metaDataEntity, FurnitureDataAccessor data, CustomFurniture config) {
        this.config = config;
        this.dataAccessor = data;
        this.metaDataEntity = metaDataEntity;
        this.metaDataEntityId = metaDataEntity.entityId();
        this.setVariantInternal(config.getVariant(data));
    }

    public Entity metaDataEntity() {
        return this.metaDataEntity;
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
     * Gets the dyed color of the furniture.
     * @return An Optional containing the dyed color, or empty if not dyed.
     */
    @NotNull
    public Optional<Color> dyedColor() {
        return this.dataAccessor.dyedColor();
    }

    /**
     * Sets the dyed color for the furniture.
     * @param color The color to apply.
     * @param affectOriginalItem If true, also updates the underlying original item; otherwise only updates the data accessor.
     */
    public void setDyedColor(Color color, boolean affectOriginalItem) {
        this.dataAccessor.setDyedColor(color);
        if (affectOriginalItem) {
            this.dataAccessor.item().ifPresent(it -> {
                Item<?> item = it.dyedColor(color);
                this.dataAccessor.setItem(item);
            });
        }
        this.refreshElements();
    }

    /**
     * Gets the firework explosion colors.
     * @return An Optional containing an array of color RGB values, or empty if not applicable.
     */
    @NotNull
    public Optional<int[]> fireworkExplosionColors() {
        return this.dataAccessor.fireworkExplosionColors();
    }

    /**
     * Sets the firework explosion colors.
     * @param colors Array of RGB color values for the explosion.
     * @param affectOriginalItem If true, also updates the underlying original item; otherwise only updates the data accessor.
     */
    public void setFireworkExplosionColors(int[] colors, boolean affectOriginalItem) {
        this.dataAccessor.setFireworkExplosionColors(colors);
        if (affectOriginalItem) {
            this.dataAccessor.item().ifPresent(it -> {
                if (!it.vanillaId().equals(ItemKeys.FIREWORK_STAR)) return;
                it.fireworkExplosion().ifPresentOrElse(firework -> {
                    it.fireworkExplosion(new FireworkExplosion(
                            firework.shape(),
                            new IntArrayList(colors),
                            firework.fadeColors(),
                            firework.hasTrail(),
                            firework.hasTwinkle()
                    ));
                }, () -> it.fireworkExplosion(new FireworkExplosion(
                        FireworkExplosion.Shape.SMALL_BALL,
                        new IntArrayList(colors),
                        new IntArrayList(),
                        false,
                        false
                )));
            });
        }
        this.refreshElements();
    }

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
        for (FurnitureElement element : this.elements) {
            element.refresh(player);
        }
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
        if (this.colliders != null) {
            for (Collider collider : this.colliders) {
                collider.destroy();
            }
        }
    }

    /**
     * Internal logic to initialize components based on a specific variant.
     * This sets up elements, hitboxes, seats, and culling data.
     */
    protected void setVariantInternal(FurnitureVariant variant) {
        this.currentVariant = variant;
        this.hitboxMap = new Int2ObjectOpenHashMap<>();
        // 初始化家具元素
        IntList virtualEntityIds = new IntArrayList();
        FurnitureElementConfig<?>[] elementConfigs = variant.elementConfigs();
        this.elements = new FurnitureElement[elementConfigs.length];
        for (int i = 0; i < elementConfigs.length; i++) {
            FurnitureElement element = elementConfigs[i].create(this);
            this.elements[i] = element;
            element.collectVirtualEntityId(virtualEntityIds::addLast);
        }
        // 初始化碰撞箱
        FurnitureHitBoxConfig<?>[] furnitureHitBoxConfigs = variant.hitBoxConfigs();
        ObjectArrayList<Collider> colliders = new ObjectArrayList<>(furnitureHitBoxConfigs.length);
        this.hitboxes = new FurnitureHitBox[furnitureHitBoxConfigs.length];
        // 辅助map，用于排除重复的座椅
        LazyReference<Map<Vector3f, Seat<FurnitureHitBox>>> seatMap = LazyReference.lazyReference(HashMap::new);
        for (int i = 0; i < furnitureHitBoxConfigs.length; i++) {
            FurnitureHitBox hitbox = furnitureHitBoxConfigs[i].create(this);
            this.hitboxes[i] = hitbox;
            for (FurnitureHitboxPart part : hitbox.parts()) {
                this.hitboxMap.put(part.entityId(), hitbox);
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
            hitbox.collectVirtualEntityId(virtualEntityIds::addLast);
            colliders.addAll(hitbox.colliders());
        }
        // 虚拟碰撞箱的实体id
        this.virtualEntityIds = virtualEntityIds.toIntArray();
        this.colliders = colliders.toArray(new Collider[0]);
        this.colliderEntityIds = colliders.stream().mapToInt(Collider::entityId).toArray();
        this.cullingData = createCullingData(variant.cullingData());
        // 外部模型
        Optional<ExternalModel> externalModel = variant.externalModel();
        if (externalModel.isPresent()) {
            this.hasExternalModel = true;
            try {
                externalModel.get().bindModel((AbstractEntity) this.metaDataEntity);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to load external model for furniture " + id(), e);
            }
        } else {
            this.hasExternalModel = false;
        }
    }

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
            return new CullingData(getMaxAABB(aabbs), parent.maxDistance, parent.aabbExpansion, parent.rayTracing);
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
    private static @NotNull AABB getMaxAABB(List<AABB> aabbs) {
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;
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
        return this.hitboxMap.get(entityId);
    }

    @Nullable
    @Override
    public CullingData cullingData() {
        return this.cullingData;
    }

    public Key id() {
        return this.config.id();
    }

    public int[] virtualEntityIds() {
        return this.virtualEntityIds;
    }

    public int[] colliderEntityIds() {
        return colliderEntityIds;
    }

    public UUID uuid() {
        return this.metaDataEntity.uuid();
    }

    @Override
    public void show(Player player) {
        for (FurnitureElement element : this.elements) {
            if (element != null) {
                element.show(player);
            }
        }
        for (FurnitureHitBox hitbox : this.hitboxes) {
            if (hitbox != null) {
                hitbox.show(player);
            }
        }
    }

    @Override
    public void hide(Player player) {
        for (FurnitureElement element : this.elements) {
            if (element != null) {
                element.hide(player);
            }
        }
        for (FurnitureHitBox hitbox : this.hitboxes) {
            if (hitbox != null) {
                hitbox.hide(player);
            }
        }
    }

    public abstract void addCollidersToWorld();

    /**
     * Destroys all seats associated with this furniture.
     */
    public void destroySeats() {
        for (FurnitureHitBox hitbox : this.hitboxes) {
            for (Seat<FurnitureHitBox> seat : hitbox.seats()) {
                seat.destroy();
            }
        }
    }

    public boolean isValid() {
        return this.metaDataEntity.isValid();
    }

    /** Fully removes the furniture from the world and cleans up resources. */
    public abstract void destroy();

    /**
     * Gets the configuration of this furniture.
     *
     * @return The {@link CustomFurniture} configuration.
     */
    public CustomFurniture config() {
        return this.config;
    }

    /**
     * Alias for {@link #config()}.
     *
     * @return The {@link CustomFurniture} configuration.
     */
    public CustomFurniture furniture() {
        return this.config;
    }

    /**
     * Gets the data accessor for this specific furniture instance.
     *
     * @return The {@link FurnitureDataAccessor} for this instance.
     */
    public FurnitureDataAccessor dataAccessor() {
        return this.dataAccessor;
    }

    /**
     * Gets the collection of physical colliders associated with this furniture.
     * <p>
     * Colliders are the invisible physical boundaries used by the server's
     * physics engine to handle movement obstruction, projectile impacts,
     * and player collision.
     * </p>
     */
    public Collider[] colliders() {
        return this.colliders;
    }

    public WorldPosition position() {
        return this.metaDataEntity.position();
    }

    public int entityId() {
        return this.metaDataEntityId;
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
     * Retrieves all visual elements associated with this furniture.
     * These elements handle the model rendering, animations, and client-side displays.
     * * @return An array of {@link FurnitureElement} currently active for this furniture instance.
     */
    public FurnitureElement[] elements() {
        return elements;
    }

    /**
     * Retrieves all functional hitboxes associated with this furniture.
     * * @return An array of {@link FurnitureHitBox} defining the physical interaction bounds.
     */
    public FurnitureHitBox[] hitboxes() {
        return hitboxes;
    }

    public World world() {
        return this.metaDataEntity.world();
    }

    /**
     * Gets the set of players who are currently "tracking" this furniture.
     */
    public abstract Set<Player> getTrackedBy();
}
