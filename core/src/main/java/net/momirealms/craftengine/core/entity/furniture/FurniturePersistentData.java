package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FurniturePersistentData {
    public static final String ITEM = "item";
    public static final String VARIANT = "variant";
    public static final String CUSTOM_DATA = "data";
    @ApiStatus.Obsolete
    public static final String ANCHOR_TYPE = "anchor_type";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final CompoundTag data;
    private boolean unsaved;

    public FurniturePersistentData(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public static FurniturePersistentData of(CompoundTag data) {
        return new FurniturePersistentData(data);
    }

    public static FurniturePersistentData ofVariant(String variant) {
        FurniturePersistentData accessor = new FurniturePersistentData(new CompoundTag());
        accessor.setVariant(variant);
        return accessor;
    }

    public CompoundTag copyTag() {
        try {
            this.readLock.lock();
            return this.data.copy();
        } finally {
            this.readLock.unlock();
        }
    }

    @ApiStatus.Internal
    public CompoundTag unsafeTag() {
        return this.data;
    }

    public void addTag(String key, Tag value) {
        if (value == null) {
            this.removeTag(key);
            return;
        }
        try {
            this.writeLock.lock();
            Tag previous = this.data.put(key, value);
            if (!Objects.equals(value, previous)) {
                this.unsaved = true;
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Nullable
    public Tag getTag(String key) {
        try {
            this.readLock.lock();
            return this.data.get(key);
        } finally {
            this.readLock.unlock();
        }
    }

    public boolean hasTag(String key) {
        try {
            this.readLock.lock();
            return this.data.containsKey(key);
        } finally {
            this.readLock.unlock();
        }
    }

    public void removeTag(String key) {
        try {
            this.writeLock.lock();
            if (this.data.containsKey(key)) {
                this.data.remove(key);
                this.unsaved = true;
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    public Optional<Item> item() {
        byte[] data;
        try {
            this.readLock.lock();
            data = this.data.getByteArray(ITEM);
        } finally {
            this.readLock.unlock();
        }
        if (data == null) return Optional.empty();
        try {
            return Optional.of(CraftEngine.instance().itemManager().fromBytes(data));
        } catch (Exception e) {
            Debugger.FURNITURE.warn(() -> "Failed to read furniture item data", e);
            return Optional.empty();
        }
    }

    public void setItem(Item item) {
        if (item == null) {
            this.removeTag(ITEM);
        } else {
            this.addTag(ITEM, NBT.createByteArray(item.toBytes()));
        }
    }

    public Optional<String> variant() {
        try {
            this.readLock.lock();
            return Optional.ofNullable(this.data.getString(VARIANT));
        } finally {
            this.readLock.unlock();
        }
    }

    public void setVariant(String variant) {
        this.addTag(VARIANT, NBT.createString(variant));
    }

    @ApiStatus.Obsolete
    public Optional<AnchorType> anchorType() {
        try {
            this.readLock.lock();
            if (this.data.containsKey(ANCHOR_TYPE)) return Optional.of(AnchorType.byId(this.data.getInt(ANCHOR_TYPE)));
            return Optional.empty();
        } finally {
            this.readLock.unlock();
        }
    }

    public static FurniturePersistentData fromBytes(final byte[] data) throws IOException {
        return new FurniturePersistentData(NBT.fromBytes(data));
    }

    public byte[] toBytes() throws IOException {
        try {
            this.readLock.lock();
            return NBT.toBytes(data);
        } finally {
            this.readLock.unlock();
        }
    }

    public boolean isUnsaved() {
        return this.unsaved;
    }

    public void markUnsaved() {
        this.unsaved = true;
    }

    public void clearUnsavedFlag() {
        this.unsaved = false;
    }
}
