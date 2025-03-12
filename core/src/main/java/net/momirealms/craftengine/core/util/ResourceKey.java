package net.momirealms.craftengine.core.util;

public class ResourceKey<T> {
    private final Key registry;
    private final Key location;

    public ResourceKey(Key registry, Key location) {
        this.registry = registry;
        this.location = location;
    }

    public static <T> ResourceKey<T> create(Key registry, Key location) {
        return new ResourceKey<>(registry, location);
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
}
