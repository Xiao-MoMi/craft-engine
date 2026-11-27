package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.StringUtils;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class NetworkItemData {
    private final Item original;
    private CompoundTag tag;

    public NetworkItemData(Item original) {
        this.original = original;
    }

    public boolean contains(String key) {
        return this.tag != null && this.tag.containsKey(key);
    }

    public void saveComponent(Key component) {
        String key = component.asString();
        if (this.contains(key)) {
            return;
        }
        Tag previous = this.original.getComponentAsSparrowTag(component);
        this.save(key, previous == null ? NetworkItemHandler.Operation.REMOVE : NetworkItemHandler.Operation.ADD, previous);
    }

    public void saveComponentPatch(Key component) {
        String key = component.asString();
        if (this.contains(key)) {
            return;
        }
        if (!this.original.hasComponent(component)) {
            this.save(key, NetworkItemHandler.Operation.REMOVE, null);
        } else if (!this.original.hasNonDefaultComponent(component)) {
            this.save(key, NetworkItemHandler.Operation.RESET, null);
        } else {
            this.save(key, NetworkItemHandler.Operation.ADD, this.original.getComponentAsSparrowTag(component));
        }
    }

    public void saveTag(String key) {
        if (!this.contains(key)) {
            this.saveTag(key, (Object[]) StringUtils.splitByDot(key));
        }
    }

    public void saveTag(String key, Object... path) {
        if (this.contains(key)) {
            return;
        }
        Tag previous = this.original.getSparrowTag(path);
        this.save(key, previous == null ? NetworkItemHandler.Operation.REMOVE : NetworkItemHandler.Operation.ADD, previous);
    }

    public void save(String key, NetworkItemHandler.Operation operation, @Nullable Tag value) {
        if (this.contains(key)) {
            return;
        }
        this.get().put(key, NetworkItemHandler.pack(operation, value));
    }

    public CompoundTag get() {
        if (this.tag == null) {
            this.tag = new CompoundTag();
        }
        return this.tag;
    }

    @Nullable
    public CompoundTag tag() {
        return this.tag;
    }
}
