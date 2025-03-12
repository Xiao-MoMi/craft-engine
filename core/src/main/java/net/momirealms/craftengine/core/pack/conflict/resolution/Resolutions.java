package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class Resolutions {
    public static final Key RETAIN_MATCHING = Key.of("craftengine:retain_matching");
    public static final Key MERGE_JSON = Key.of("craftengine:merge_json");
    public static final Key CONDITIONAL = Key.of("craftengine:conditional");

    static {
        register(RETAIN_MATCHING, RetainMatchingResolution.FACTORY);
        register(MERGE_JSON, MergeJsonResolution.FACTORY);
        register(CONDITIONAL, ConditionalResolution.FACTORY);
    }

    public static void register(Key key, ResolutionFactory factory) {
        Holder.Reference<ResolutionFactory> holder = ((WritableRegistry<ResolutionFactory>) BuiltInRegistries.RESOLUTION_FACTORY).registerForHolder(new ResourceKey<>(Registries.RESOLUTION_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static Resolution fromMap(Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "empty");
        if (type == null) {
            throw new NullPointerException("path matcher type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        ResolutionFactory factory = BuiltInRegistries.RESOLUTION_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown matcher type: " + type);
        }
        return factory.create(map);
    }
}
