package net.momirealms.craftengine.core.plugin.text.component;

import net.kyori.adventure.key.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTDataComponentValue;

import java.util.HashMap;
import java.util.Map;

public final class NBTDataComponentPatch {
    private static final String REMOVED_PREFIX = "!";

    private NBTDataComponentPatch() {
    }

    public static CompoundTag encode(Map<Key, NBTDataComponentValue> components) {
        CompoundTag encoded = new CompoundTag();
        for (Map.Entry<Key, NBTDataComponentValue> entry : components.entrySet()) {
            NBTDataComponentValue value = entry.getValue();
            String key = entry.getKey().asMinimalString();
            encoded.put(value.isRemoved() ? REMOVED_PREFIX + key : key, value.tag());
        }
        return encoded;
    }

    @SuppressWarnings("all")
    public static Map<Key, NBTDataComponentValue> decode(CompoundTag components) {
        Map<Key, NBTDataComponentValue> decoded = new HashMap<>();
        for (Map.Entry<String, Tag> entry : components.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(REMOVED_PREFIX)) {
                decoded.put(Key.key(key.substring(REMOVED_PREFIX.length())), NBTDataComponentValue.removed());
            } else {
                decoded.put(Key.key(key), NBTDataComponentValue.of(entry.getValue()));
            }
        }
        return decoded;
    }
}
