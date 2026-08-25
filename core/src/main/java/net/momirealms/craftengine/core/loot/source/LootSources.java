package net.momirealms.craftengine.core.loot.source;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class LootSources {
    private static final LootSource.Factory WILDCARD_FACTORY = new LootSource.Factory();
    private static final TargetedLootSource.Factory TARGETED_FACTORY = new TargetedLootSource.Factory();

    public static final LootSourceType<LootSource> FISHING = register(Key.ce("fishing"), WILDCARD_FACTORY);
    public static final LootSourceType<LootSource> PIGLIN_BARTER = register(Key.ce("piglin_barter"), WILDCARD_FACTORY);
    public static final LootSourceType<TargetedLootSource> BLOCK_BREAK = register(Key.ce("block_break"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> ENTITY_DEATH = register(Key.ce("entity_death"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> CONTAINER = register(Key.ce("container"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> ARCHAEOLOGY = register(Key.ce("archaeology"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> ENTITY_DROP = register(Key.ce("entity_drop"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> HARVEST = register(Key.ce("harvest"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> BLOCK_SHEAR = register(Key.ce("block_shear"), TARGETED_FACTORY);
    /**
     * @deprecated Use {@link #BLOCK_SHEAR} instead.
     */
    @Deprecated
    public static final LootSourceType<TargetedLootSource> SHEAR_BLOCK = BLOCK_SHEAR;
    public static final LootSourceType<TargetedLootSource> ENTITY_SHEAR = register(Key.ce("entity_shear"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> VAULT = register(Key.ce("vault"), TARGETED_FACTORY);
    public static final LootSourceType<TargetedLootSource> ADVANCEMENT = register(Key.ce("advancement"), TARGETED_FACTORY);

    private LootSources() {}

    public static <T extends LootSource> LootSourceType<T> register(Key key, LootSourceFactory<T> factory) {
        LootSourceType<T> type = new LootSourceType<>(key, factory);
        ((WritableRegistry<LootSourceType<? extends LootSource>>) BuiltInRegistries.LOOT_SOURCE_TYPE)
                .register(ResourceKey.create(Registries.LOOT_SOURCE_TYPE.location(), key), type);
        return type;
    }

    public static LootSource fromConfig(Key id, ConfigSection section) {
        String type = section.getNonEmptyString("type");
        // 兼容旧版类型别名（包括 vanilla-loots）
        type = switch (type) {
            case "block" -> "block_break";
            case "entity" -> "entity_death";
            case "shear_block" -> "block_shear";
            default -> type;
        };
        Key key = Key.ce(type);
        LootSourceType<?> sourceType = BuiltInRegistries.LOOT_SOURCE_TYPE.getValue(key);
        if (sourceType == null) {
            throw new KnownResourceException("loot.source.unknown_type", section.assemblePath("type"), type);
        }
        @SuppressWarnings("unchecked")
        LootSourceFactory<LootSource> factory = (LootSourceFactory<LootSource>) sourceType.factory();
        return factory.create(sourceType, id, section);
    }
}
