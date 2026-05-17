package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.function.Function;

public final class PlayerSelectors {
    public static final PlayerSelectorType<? extends Context> ALL = register(Key.ce("all"), AllPlayerSelector.factory());
    public static final PlayerSelectorType<? extends Context> SELF = register(Key.ce("self"), SelfPlayerSelector.factory());

    private PlayerSelectors() {}

    public static <CTX extends Context> PlayerSelectorType<CTX> register(Key key, PlayerSelectorFactory<CTX> factory) {
        PlayerSelectorType<CTX> type = new PlayerSelectorType<>(key, factory);
        ((WritableRegistry<PlayerSelectorType<? extends Context>>) BuiltInRegistries.PLAYER_SELECTOR_TYPE)
                .register(ResourceKey.create(Registries.PLAYER_SELECTOR_TYPE.location(), key), type);
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <CTX extends Context> PlayerSelector<CTX> fromConfig(ConfigSection section, Function<ConfigSection, Condition<CTX>> conditionFactory) {
        String type = section.getNonEmptyString("type");
        Key key = Key.ce(type);
        PlayerSelectorType<CTX> selectorType = (PlayerSelectorType<CTX>) BuiltInRegistries.PLAYER_SELECTOR_TYPE.getValue(key);
        if (selectorType == null) {
            throw new KnownResourceException("player_selector.unknown_type", section.assemblePath("type"), key.asString());
        }
        return selectorType.factory().create(section, conditionFactory);
    }
}
