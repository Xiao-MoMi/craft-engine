package net.momirealms.craftengine.bukkit.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;

import java.util.function.Consumer;

public final class LegacyDFUUtils {

    private LegacyDFUUtils() {}

    public static <T, D> T parse(Codec<T> codec, DynamicOps<D> ops, D input, Consumer<String> onError) {
        return codec.parse(ops, input).getOrThrow(true, onError);
    }
}
