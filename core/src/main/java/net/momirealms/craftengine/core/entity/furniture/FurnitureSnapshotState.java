package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.util.CustomDataType;
import net.momirealms.craftengine.core.world.World;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class FurnitureSnapshotState {
    protected final List<FurnitureElement> elements;
    protected final List<FurnitureHitBox> hitboxes;
    protected final Int2ObjectMap<FurnitureHitBox> hitboxMap;
    protected final List<Collider> colliders;
    // 按需创建；家具线程写入后发布，供异步追踪回调读取。
    protected volatile Map<CustomDataType<?>, Object> customData;

    public FurnitureSnapshotState(List<FurnitureElement> elements,
                                  List<FurnitureHitBox> hitboxes,
                                  Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                  List<Collider> colliders) {
        this.elements = elements;
        this.hitboxes = hitboxes;
        this.hitboxMap = hitboxMap;
        this.colliders = colliders;
    }

    protected abstract void addCollidersToWorld(World world);

    public void refreshElements(Player player) {
        for (int elementIndex = 0, elementCount = this.elements.size(); elementIndex < elementCount; elementIndex++) {
            FurnitureElement element = this.elements.get(elementIndex);
            element.update(player);
        }
    }

    public void clearColliders() {
        if (this.colliders != null) {
            for (int colliderIndex = 0, colliderCount = this.colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
                Collider collider = this.colliders.get(colliderIndex);
                collider.destroy();
            }
        }
    }

    public void hideHitboxes(Player player) {
        if (this.hitboxes != null) {
            for (int hitboxIndex = 0, hitboxCount = this.hitboxes.size(); hitboxIndex < hitboxCount; hitboxIndex++) {
                FurnitureHitBox hitbox = this.hitboxes.get(hitboxIndex);
                hitbox.hide(player);
            }
        }
    }

    public void showHitboxes(Player player) {
        if (this.hitboxes != null) {
            for (int hitboxIndex = 0, hitboxCount = this.hitboxes.size(); hitboxIndex < hitboxCount; hitboxIndex++) {
                FurnitureHitBox hitbox = this.hitboxes.get(hitboxIndex);
                hitbox.show(player);
            }
        }
    }

    public FurnitureHitBox hitboxByEntityId(int entityId) {
        return this.hitboxMap.get(entityId);
    }

    public void show(Player player) {
        for (int elementIndex = 0, elementCount = this.elements.size(); elementIndex < elementCount; elementIndex++) {
            FurnitureElement element = this.elements.get(elementIndex);
            if (element != null) {
                element.show(player);
            }
        }
        for (int hitboxIndex = 0, hitboxCount = this.hitboxes.size(); hitboxIndex < hitboxCount; hitboxIndex++) {
            FurnitureHitBox hitbox = this.hitboxes.get(hitboxIndex);
            if (hitbox != null) {
                hitbox.show(player);
            }
        }
    }

    public void hide(Player player) {
        for (int elementIndex = 0, elementCount = this.elements.size(); elementIndex < elementCount; elementIndex++) {
            FurnitureElement element = this.elements.get(elementIndex);
            if (element != null) {
                element.hide(player);
            }
        }
        for (int hitboxIndex = 0, hitboxCount = this.hitboxes.size(); hitboxIndex < hitboxCount; hitboxIndex++) {
            FurnitureHitBox hitbox = this.hitboxes.get(hitboxIndex);
            if (hitbox != null) {
                hitbox.hide(player);
            }
        }
    }

    public void destroySeats() {
        for (int hitboxIndex = 0, hitboxCount = this.hitboxes.size(); hitboxIndex < hitboxCount; hitboxIndex++) {
            FurnitureHitBox hitbox = this.hitboxes.get(hitboxIndex);
            for (Seat<SeatOwner> seat : hitbox.seats()) {
                seat.destroy();
            }
        }
    }

    public List<FurnitureElement> elements() {
        return Collections.unmodifiableList(this.elements);
    }

    public List<FurnitureHitBox> hitboxes() {
        return Collections.unmodifiableList(this.hitboxes);
    }

    public List<Collider> colliders() {
        return Collections.unmodifiableList(this.colliders);
    }

    public Map<CustomDataType<?>, Object> customData() {
        Map<CustomDataType<?>, Object> customData = this.customData;
        return customData == null ? Collections.emptyMap() : Collections.unmodifiableMap(customData);
    }

    public <T> void setCustomData(CustomDataType<T> contextKey, T value) {
        Map<CustomDataType<?>, Object> customData = this.customData;
        if (customData == null) {
            customData = new IdentityHashMap<>(4);
        }
        customData.put(contextKey, value);
        this.customData = customData;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(CustomDataType<T> contextKey) {
        Map<CustomDataType<?>, Object> customData = this.customData;
        return customData == null ? null : (T) customData.get(contextKey);
    }
}
