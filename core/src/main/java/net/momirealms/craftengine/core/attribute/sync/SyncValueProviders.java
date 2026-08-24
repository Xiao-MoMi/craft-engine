package net.momirealms.craftengine.core.attribute.sync;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class SyncValueProviders {
    public static final SyncValueProviderType<ExpressionSyncValueProvider> EXPRESSION = register(Key.ce("expression"), ExpressionSyncValueProvider.FACTORY);
    public static final SyncValueProviderType<DeltaSyncValueProvider> DELTA = register(Key.ce("delta"), DeltaSyncValueProvider.FACTORY);
    public static final SyncValueProviderType<RatioSyncValueProvider> RATIO = register(Key.ce("ratio"), RatioSyncValueProvider.FACTORY);

    private SyncValueProviders() {}

    public static <T extends SyncValueProvider> SyncValueProviderType<T> register(Key key, SyncValueProviderFactory<T> factory) {
        SyncValueProviderType<T> type = new SyncValueProviderType<>(key, factory);
        ((WritableRegistry<SyncValueProviderType<? extends SyncValueProvider>>) BuiltInRegistries.SYNC_VALUE_PROVIDER_TYPE)
                .register(ResourceKey.create(Registries.SYNC_VALUE_PROVIDER_TYPE.location(), key), type);
        return type;
    }

    public static SyncValueProvider fromConfig(ConfigValue value) {
        if (value.is(Map.class)) {
            return fromConfig(value.getAsSection());
        }
        return new ExpressionSyncValueProvider(value.path(), value.getAsString());
    }

    public static SyncValueProvider fromConfig(ConfigSection section) {
        String type = section.getString("type", "expression");
        Key key = Key.ce(type);
        SyncValueProviderType<? extends SyncValueProvider> providerType = BuiltInRegistries.SYNC_VALUE_PROVIDER_TYPE.getValue(key);
        if (providerType == null) {
            throw new KnownResourceException("attribute.sync_value_provider.unknown_type", section.assemblePath("type"), type);
        }
        return providerType.factory().create(section);
    }
}
