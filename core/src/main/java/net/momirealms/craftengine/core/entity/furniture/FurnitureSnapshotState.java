package net.momirealms.craftengine.core.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.util.CustomDataType;
import net.momirealms.craftengine.core.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class FurnitureSnapshotState {
    protected final List<FurnitureElement> elements;
    protected final List<FurnitureHitBox> hitboxes;
    protected final Int2ObjectMap<FurnitureHitBox> hitboxMap;
    protected final List<Collider> colliders;
    protected final Map<CustomDataType<?>, Object> customData;

    public FurnitureSnapshotState(List<FurnitureElement> elements,
                                  List<FurnitureHitBox> hitboxes,
                                  Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                  List<Collider> colliders,
                                  Map<CustomDataType<?>, Object> customData) {
        this.elements = elements;
        this.hitboxes = hitboxes;
        this.hitboxMap = hitboxMap;
        this.colliders = colliders;
        this.customData = customData;
    }

    protected abstract void addCollidersToWorld(World world);

    public void refreshElements(Player player) {
        for (FurnitureElement element : this.elements) {
            element.refresh(player);
        }
    }

    public void clearColliders() {
        if (this.colliders != null) {
            for (Collider collider : this.colliders) {
                collider.destroy();
            }
        }
    }

    public FurnitureHitBox hitboxByEntityId(int entityId) {
        return this.hitboxMap.get(entityId);
    }

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

    public void destroySeats() {
        for (FurnitureHitBox hitbox : this.hitboxes) {
            for (Seat<FurnitureHitBox> seat : hitbox.seats()) {
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
        return Collections.unmodifiableMap(this.customData);
    }

    public <T> void setCustomData(CustomDataType<T> contextKey, T value) {
        this.customData.put(contextKey, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(CustomDataType<T> contextKey) {
        return (T) this.customData.get(contextKey);
    }
}
