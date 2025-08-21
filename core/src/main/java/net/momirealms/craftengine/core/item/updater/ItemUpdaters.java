package net.momirealms.craftengine.core.item.updater;

import net.momirealms.craftengine.core.item.updater.impl.ApplyDataOperation;
import net.momirealms.craftengine.core.item.updater.impl.ResetOperation;
import net.momirealms.craftengine.core.item.updater.impl.TransmuteOperation;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

@SuppressWarnings("unchecked")
public class ItemUpdaters {
    public static final Key APPLY_DATA = Key.of("craftengine:apply_data");
    public static final Key TRANSMUTE = Key.of("craftengine:transmute");
    public static final Key RESET = Key.of("craftengine:reset");

    static {
        register(APPLY_DATA, ApplyDataOperation.TYPE);
        register(TRANSMUTE, TransmuteOperation.TYPE);
        register(RESET, ResetOperation.TYPE);
    }

    public static void register(Key id, ItemUpdaterType<?> type) {
        WritableRegistry<ItemUpdaterType<?>> registry = (WritableRegistry<ItemUpdaterType<?>>) BuiltInRegistries.ITEM_UPDATER_TYPE;
        registry.register(ResourceKey.create(Registries.ITEM_UPDATER_TYPE.location(), id), type);
    }

    public static <I> ItemUpdater<I> fromMap(Key item, Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.item.updater.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        ItemUpdaterType<I> updaterType = (ItemUpdaterType<I>) BuiltInRegistries.ITEM_UPDATER_TYPE.getValue(key);
        if (updaterType == null) {
            throw new LocalizedResourceConfigException("warning.config.item.updater.invalid_type", type);
        }
        return updaterType.create(item, map);
    }
}
