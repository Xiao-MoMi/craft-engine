package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class Resolutions {
    public static final Key RETAIN_MATCHING = Key.of("craftengine:retain_matching");
    public static final Key MERGE_JSON = Key.of("craftengine:merge_json");
    public static final Key MERGE_ATLAS = Key.of("craftengine:merge_atlas");
    public static final Key CONDITIONAL = Key.of("craftengine:conditional");
    public static final Key MERGE_PACK_MCMETA = Key.of("craftengine:merge_pack_mcmeta");

    static {
        register(RETAIN_MATCHING, RetainMatchingResolution.FACTORY);
        register(MERGE_JSON, ResolutionMergeJson.FACTORY);
        register(CONDITIONAL, ResolutionConditional.FACTORY);
        register(MERGE_PACK_MCMETA, ResolutionMergePackMcMeta.FACTORY);
        register(MERGE_ATLAS, ResolutionMergeAltas.FACTORY);
    }

    public static void register(Key key, ResolutionFactory factory) {
        ((WritableRegistry<ResolutionFactory>) BuiltInRegistries.RESOLUTION_FACTORY).register(ResourceKey.create(Registries.RESOLUTION_FACTORY.location(), key), factory);
    }

    public static Resolution fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), () -> new LocalizedException("warning.config.conflict_resolution.missing_type"));
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        ResolutionFactory factory = BuiltInRegistries.RESOLUTION_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedException("warning.config.conflict_resolution.invalid_type", type);
        }
        return factory.create(map);
    }
}
