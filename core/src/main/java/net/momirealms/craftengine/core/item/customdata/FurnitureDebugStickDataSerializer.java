package net.momirealms.craftengine.core.item.customdata;

import net.momirealms.craftengine.core.entity.furniture.FurnitureDebugStickState;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Locale;

public final class FurnitureDebugStickDataSerializer implements CustomDataSerializer<FurnitureDebugStickData> {
    public static final FurnitureDebugStickDataSerializer INSTANCE = new FurnitureDebugStickDataSerializer();

    private FurnitureDebugStickDataSerializer() {}

    @Override
    public Tag serialize(FurnitureDebugStickData data) {
        CompoundTag tag = new CompoundTag();
        tag.putString("property", data.state.toString().toLowerCase(Locale.ROOT));
        return tag;
    }

    @Override
    public FurnitureDebugStickData deserialize(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            String state = compoundTag.getString("property");
            if (state != null) {
                try {
                    FurnitureDebugStickState stateEnum = FurnitureDebugStickState.valueOf(state.toUpperCase(Locale.ROOT));
                    return new FurnitureDebugStickData(stateEnum);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }
}
