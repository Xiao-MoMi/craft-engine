package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;

import java.io.IOException;
import java.util.Optional;

public class FurnitureExtraData {
    public static final String ITEM = "item";
    public static final String DYED_COLOR = "dyed_color";
    public static final String ANCHOR_TYPE = "anchor_type";

    private final CompoundTag data;

    public FurnitureExtraData(CompoundTag data) {
        this.data = data;
    }

    public static FurnitureExtraData of(CompoundTag data) {
        return new FurnitureExtraData(data);
    }

    public CompoundTag copyTag() {
        return this.data.copy();
    }

    public CompoundTag unsafeTag() {
        return this.data;
    }

    public Optional<Item<?>> item() {
        byte[] data = this.data.getByteArray(ITEM);
        if (data == null) return Optional.empty();
        return Optional.of(CraftEngine.instance().itemManager().fromByteArray(data));
    }

    public Optional<Integer> dyedColor() {
        if (this.data.containsKey(DYED_COLOR)) return Optional.of(this.data.getInt(DYED_COLOR));
        return Optional.empty();
    }

    public Optional<AnchorType> anchorType() {
        if (this.data.containsKey(ANCHOR_TYPE)) return Optional.of(AnchorType.byId(this.data.getInt(ANCHOR_TYPE)));
        return Optional.empty();
    }

    public FurnitureExtraData anchorType(AnchorType type) {
        this.data.putInt(ANCHOR_TYPE, type.getId());
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FurnitureExtraData fromBytes(final byte[] data) throws IOException {
        return new FurnitureExtraData(NBT.fromBytes(data));
    }

    public static byte[] toBytes(final FurnitureExtraData data) throws IOException {
        return NBT.toBytes(data.data);
    }

    public byte[] toBytes() throws IOException {
        return toBytes(this);
    }

    public static class Builder {
        private final CompoundTag data;

        public Builder() {
            this.data = new CompoundTag();
        }

        public Builder item(Item<?> item) {
            this.data.putByteArray(ITEM, item.toByteArray());
            return this;
        }

        public Builder dyedColor(Integer color) {
            if (color == null) return this;
            this.data.putInt(DYED_COLOR, color);
            return this;
        }

        public Builder anchorType(AnchorType type) {
            this.data.putInt(ANCHOR_TYPE, type.getId());
            return this;
        }

        public FurnitureExtraData build() {
            return new FurnitureExtraData(data);
        }
    }
}
