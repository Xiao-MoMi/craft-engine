package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

public final class FurnitureDataAccessor {
    public static final String ITEM = "item";
    public static final String DYED_COLOR = "dyed_color";
    public static final String FIREWORK_EXPLOSION_COLORS = "firework_explosion_colors";
    public static final String VARIANT = "variant";
    @ApiStatus.Obsolete
    public static final String ANCHOR_TYPE = "anchor_type";

    private final CompoundTag data;

    public FurnitureDataAccessor(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public static FurnitureDataAccessor of(CompoundTag data) {
        return new FurnitureDataAccessor(data);
    }

    public static FurnitureDataAccessor ofVariant(String variant) {
        FurnitureDataAccessor accessor = new FurnitureDataAccessor(new CompoundTag());
        accessor.setVariant(variant);
        return accessor;
    }

    public CompoundTag copyTag() {
        return this.data.copy();
    }

    @ApiStatus.Internal
    public CompoundTag unsafeTag() {
        return this.data;
    }

    public void addCustomData(String key, Tag value) {
        this.data.put(key, value);
    }

    @Nullable
    public Tag getCustomData(String key) {
        return this.data.get(key);
    }

    public void removeCustomData(String key) {
        this.data.remove(key);
    }

    public Optional<Item<?>> item() {
        byte[] data = this.data.getByteArray(ITEM);
        if (data == null) return Optional.empty();
        try {
            return Optional.of(CraftEngine.instance().itemManager().fromByteArray(data));
        } catch (Exception e) {
            Debugger.FURNITURE.warn(() -> "Failed to read furniture item data", e);
            return Optional.empty();
        }
    }

    public void setItem(Item<?> item) {
        this.data.putByteArray(ITEM, item.toByteArray());
    }

    public FurnitureColorSource getColorSource() {
        return new FurnitureColorSource(dyedColor().orElse(null), fireworkExplosionColors().orElse(null));
    }

    public Optional<int[]> fireworkExplosionColors() {
        if (this.data.containsKey(FIREWORK_EXPLOSION_COLORS)) return Optional.of(this.data.getIntArray(FIREWORK_EXPLOSION_COLORS));
        return Optional.empty();
    }

    public void setFireworkExplosionColors(int[] colors) {
        if (colors == null) {
            this.data.remove(FIREWORK_EXPLOSION_COLORS);
            return;
        }
        this.data.putIntArray(FIREWORK_EXPLOSION_COLORS, colors);
    }

    public Optional<Color> dyedColor() {
        if (this.data.containsKey(DYED_COLOR)) return Optional.of(Color.fromDecimal(this.data.getInt(DYED_COLOR)));
        return Optional.empty();
    }

    public void setDyedColor(@Nullable Color color) {
        if (color == null) {
            this.data.remove(DYED_COLOR);
            return;
        }
        this.data.putInt(DYED_COLOR, color.color());
    }

    public Optional<String> variant() {
        return Optional.ofNullable(this.data.getString(VARIANT));
    }

    public void setVariant(String variant) {
        this.data.putString(VARIANT, variant);
    }

    @ApiStatus.Obsolete
    public Optional<AnchorType> anchorType() {
        if (this.data.containsKey(ANCHOR_TYPE)) return Optional.of(AnchorType.byId(this.data.getInt(ANCHOR_TYPE)));
        return Optional.empty();
    }

    @ApiStatus.Obsolete
    public FurnitureDataAccessor anchorType(AnchorType type) {
        this.data.putInt(ANCHOR_TYPE, type.getId());
        return this;
    }

    public static FurnitureDataAccessor fromBytes(final byte[] data) throws IOException {
        return new FurnitureDataAccessor(NBT.fromBytes(data));
    }

    public static byte[] toBytes(final FurnitureDataAccessor data) throws IOException {
        return NBT.toBytes(data.data);
    }

    public byte[] toBytes() throws IOException {
        return toBytes(this);
    }
}
