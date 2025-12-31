package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.entity.Cullable;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitboxPart;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.entityculling.CullingData;
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
    public final FurnitureDataAccessor dataAccessor;
    public final Entity metaDataEntity;
    public final int metaDataEntityId;

    protected CullingData cullingData;
    protected FurnitureVariant currentVariant;
    protected FurnitureElement[] elements;
    protected Collider[] colliders;
    protected FurnitureHitBox[] hitboxes;
    protected Int2ObjectMap<FurnitureHitBox> hitboxMap;
    protected int[] virtualEntityIds;
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

    public FurnitureVariant getCurrentVariant() {
        return this.currentVariant;
    }

    public boolean setVariant(String variantName) {
        return this.setVariant(variantName, false);
    }

    public abstract boolean setVariant(String variantName, boolean force);

    public CompletableFuture<Boolean> moveTo(WorldPosition position) {
        return this.moveTo(position, false);
    }

    public abstract CompletableFuture<Boolean> moveTo(WorldPosition position, boolean force);

    public abstract void refresh();

    public abstract void refresh(Player player);

    protected void clearColliders() {
        if (this.colliders != null) {
            for (Collider collider : this.colliders) {
                collider.destroy();
            }
        }
    }

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

    // 会发给玩家的包
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
        this.config.behavior().asyncShow(this, player);
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
        this.config.behavior().asyncHide(this, player);
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

    public abstract void destroy();

    public CustomFurniture config() {
        return this.config;
    }

    public FurnitureDataAccessor dataAccessor() {
        return this.dataAccessor;
    }

    public Collider[] colliders() {
        return this.colliders;
    }

    public WorldPosition position() {
        return this.metaDataEntity.position();
    }

    public int entityId() {
        return this.metaDataEntityId;
    }

    public boolean hasExternalModel() {
        return this.hasExternalModel;
    }

    public Vec3d getRelativePosition(Vector3f position) {
        return getRelativePosition(this.position(), position);
    }

    public static Vec3d getRelativePosition(WorldPosition location, Vector3f position) {
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0f, (float) Math.toRadians(180 - location.yRot()), 0f).conjugate();
        Vector3f offset = conjugated.transform(new Vector3f(position));
        return new Vec3d(location.x + offset.x, location.y + offset.y, location.z - offset.z);
    }

    public World world() {
        return this.metaDataEntity.world();
    }
}
