package net.momirealms.craftengine.core.item.customdata;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class CustomDataSerializers {
    private static final Map<Class<?>, CustomDataSerializer<?>> CUSTOM_DATA_SERIALIZERS = new HashMap<>();

    private CustomDataSerializers() {}

    public static void registerSerializer(final Class<?> clazz, final CustomDataSerializer<?> serializer) {
        CUSTOM_DATA_SERIALIZERS.put(clazz, serializer);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> CustomDataSerializer<T> getSerializer(final Class<T> clazz) {
        return (CustomDataSerializer<T>) CUSTOM_DATA_SERIALIZERS.get(clazz);
    }
}
