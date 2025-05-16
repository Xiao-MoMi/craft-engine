package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public class PlayerSelectors {
    public static final Key ALL = Key.of("craftengine:all");
    public static final Key SELF = Key.of("craftengine:self");

    static {
        register(ALL, new AllPlayerSelector.FactoryImpl<>());
        register(SELF, new SelfPlayerSelector.FactoryImpl<>());
    }

    public static void register(Key key, PlayerSelectorFactory<?> factory) {
        Holder.Reference<PlayerSelectorFactory<?>> holder = ((WritableRegistry<PlayerSelectorFactory<?>>) BuiltInRegistries.PLAYER_SELECTOR_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.PLAYER_SELECTOR_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    @Nullable
    public static <CTX extends Context> PlayerSelector<CTX> fromObject(Object object, Function<Map<String, Object>, Condition<CTX>> conditionFactory) {
        if (object == null) return null;
        if (object instanceof Map<?,?> map) {
            Map<String, Object> selectorMap = MiscUtils.castToMap(map, false);
            return fromMap(selectorMap, conditionFactory);
        } else if (object instanceof String target) {
            if (target.equals("all") || target.equals("@a")) {
                return new AllPlayerSelector<>();
            } else if (target.equals("self") || target.equals("@s")) {
                return new SelfPlayerSelector<>();
            }
        }
        throw new LocalizedResourceConfigException("warning.config.selector.invalid_target", object.toString());
    }

    public static <CTX extends Context> PlayerSelector<CTX> fromMap(Map<String, Object> map, Function<Map<String, Object>, Condition<CTX>> conditionFactory) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.selector.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        @SuppressWarnings("unchecked")
        PlayerSelectorFactory<CTX> factory = (PlayerSelectorFactory<CTX>) BuiltInRegistries.PLAYER_SELECTOR_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.selector.invalid_type", type);
        }
        return factory.create(map, conditionFactory);
    }
}
