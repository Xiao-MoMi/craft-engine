package net.momirealms.craftengine.core.util;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class HolderSetUtils {

    private HolderSetUtils() {
    }

    public static Either<List<Key>, Key> fromConfig(ConfigValue value) {
        if (value.is(String.class)) {
            String id = value.getAsString();
            if (id.startsWith("#")) {
                return Either.right(ConfigValue.of(value.path(), id.substring(1)).getAsIdentifier());
            }
        }
        return Either.left(value.getAsList(ConfigValue::getAsIdentifier));
    }

    @Nullable
    public static Either<List<Key>, Key> fromNBT(@Nullable Tag tag) {
        if (tag instanceof StringTag stringTag) {
            String id = stringTag.value();
            if (id.startsWith("#")) {
                return Either.right(Key.of(id.substring(1)));
            }
            return Either.left(List.of(Key.of(id)));
        } else if (tag instanceof ListTag listTag) {
            List<Key> ids = new ArrayList<>(listTag.size());
            for (int i = 0; i < listTag.size(); i++) {
                ids.add(Key.of(listTag.getString(i)));
            }
            return Either.left(ids);
        }
        return null;
    }

    public static Tag toNBT(Either<List<Key>, Key> holderSet) {
        return holderSet.map(ids -> {
            ListTag tag = new ListTag();
            for (Key id : ids) {
                tag.add(new StringTag(id.asString()));
            }
            return tag;
        }, key -> new StringTag("#" + key.asString()));
    }
}
