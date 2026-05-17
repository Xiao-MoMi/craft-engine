package net.momirealms.craftengine.core.item.customdata;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public final class BlockDebugStickDataSerializer implements CustomDataSerializer<BlockDebugStickData> {
    public static final BlockDebugStickDataSerializer INSTANCE = new BlockDebugStickDataSerializer();

    private BlockDebugStickDataSerializer() {}

    @Override
    public Tag serialize(BlockDebugStickData data) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<Key, String> entry : data.properties.entrySet()) {
            tag.putString(entry.getKey().toString(), entry.getValue());
        }
        return tag;
    }

    @Override
    public BlockDebugStickData deserialize(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            Map<Key, String> properties = new HashMap<>();
            for (Map.Entry<String, Tag> entry : compoundTag.entrySet()) {
                if (entry.getValue() instanceof StringTag stringTag) {
                    properties.put(Key.of(entry.getKey()), stringTag.value());
                }
            }
            return new BlockDebugStickData(properties);
        }
        return null;
    }
}
