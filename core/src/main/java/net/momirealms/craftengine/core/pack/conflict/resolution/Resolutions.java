package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class Resolutions {
    public static final ResolutionType<RetainMatchingResolution> RETAIN_MATCHING = register(Key.ce("retain_matching"), RetainMatchingResolution.FACTORY);
    public static final ResolutionType<MergeJsonResolution> MERGE_JSON = register(Key.ce("merge_json"), MergeJsonResolution.FACTORY);
    public static final ResolutionType<MergeAltasResolution> MERGE_ATLAS = register(Key.ce("merge_atlas"), MergeAltasResolution.FACTORY);
    public static final ResolutionType<MergeFontResolution> MERGE_FONT = register(Key.ce("merge_font"), MergeFontResolution.FACTORY);
    public static final ResolutionType<ConditionalResolution> CONDITIONAL = register(Key.ce("conditional"), ConditionalResolution.FACTORY);
    public static final ResolutionType<MergePackMcMetaResolution> MERGE_PACK_MCMETA = register(Key.ce("merge_pack_mcmeta"), MergePackMcMetaResolution.FACTORY);
    public static final ResolutionType<MergeLegacyModelResolution> MERGE_LEGACY_MODEL = register(Key.ce("merge_legacy_model"), MergeLegacyModelResolution.FACTORY);

    private Resolutions() {}

    public static <T extends Resolution> ResolutionType<T> register(Key key, ResolutionFactory<T> factory) {
        ResolutionType<T> type = new ResolutionType<>(key, factory);
        ((WritableRegistry<ResolutionType<? extends Resolution>>) BuiltInRegistries.RESOLUTION_TYPE)
                .register(ResourceKey.create(Registries.RESOLUTION_TYPE.location(), key), type);
        return type;
    }

    public static Resolution fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        ResolutionType<? extends Resolution> resolutionType = BuiltInRegistries.RESOLUTION_TYPE.getValue(key);
        if (resolutionType == null) {
            throw new KnownResourceException("resource_pack.conflict_resolution.unknown_type", section.assemblePath("type"), key.asString());
        }
        return resolutionType.factory().create(section);
    }
}
