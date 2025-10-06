package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface BlockStateWrapper extends Comparable<BlockStateWrapper> {

    Object literalObject();

    int registryId();

    Key ownerId();

    <T> T getProperty(String propertyName);

    boolean hasProperty(String propertyName);

    BlockStateWrapper withProperty(String propertyName, String propertyValue);

    String getAsString();

    @Override
    default int compareTo(@NotNull BlockStateWrapper o) {
        return Integer.compare(registryId(), o.registryId());
    }

    default BlockStateWrapper withProperties(CompoundTag properties) {
        BlockStateWrapper result = this;
        for (Map.Entry<String, Tag> entry : properties.entrySet()) {
            Tag value = entry.getValue();
            if (value instanceof StringTag stringTag) {
                result = result.withProperty(entry.getKey(), stringTag.getAsString());
            } else if (value instanceof IntTag intTag) {
                result = result.withProperty(entry.getKey(), String.valueOf(intTag.getAsInt()));
            } else if (value instanceof ByteTag byteTag) {
                result = result.withProperty(entry.getKey(), String.valueOf(byteTag.booleanValue()));
            }
        }
        return result;
    }
}
