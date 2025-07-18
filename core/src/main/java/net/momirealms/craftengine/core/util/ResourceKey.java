package net.momirealms.craftengine.core.util;

import com.google.common.collect.MapMaker;

import java.util.Map;

public class ResourceKey<T> {
    private static final Map<Internal, ResourceKey<?>> VALUES = (new MapMaker()).weakValues().makeMap();
    private final Key registry;
    private final Key location;

    private ResourceKey(Key registry, Key location) {
        this.registry = registry;
        this.location = location;
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<T> create(Key registry, Key location) {
        return (ResourceKey<T>) VALUES.computeIfAbsent(new Internal(registry, location), (key) -> new ResourceKey<>(key.registry, key.location));
    }

    public Key registry() {
        return registry;
    }

    public Key location() {
        return location;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + this.registry + " / " + this.location + "]";
    }

    record Internal(Key registry, Key location) {
    }
}
