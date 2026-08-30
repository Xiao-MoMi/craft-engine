package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.component.value.FireworkExplosion;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NumericTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class SourceItemColorDataSourceConfig implements FurnitureDataSourceConfig<Color> {
    private static final Map<Key, ColorReader> SPECIAL_READERS = Map.of(
            DataComponentKeys.DYED_COLOR, item -> item.dyedColor().orElse(null),
            DataComponentKeys.FIREWORK_EXPLOSION, SourceItemColorDataSourceConfig::fireworkColor,
            DataComponentKeys.POTION_CONTENTS, SourceItemColorDataSourceConfig::potionColor
    );

    private final List<Key> components;

    private SourceItemColorDataSourceConfig(List<Key> components) {
        this.components = List.copyOf(components);
    }

    public static @NotNull SourceItemColorDataSourceConfig create(@NotNull List<Key> components) {
        return new SourceItemColorDataSourceConfig(components);
    }

    public static @NotNull SourceItemColorDataSourceConfig fromConfig(@NotNull ConfigValue value) {
        if (!value.is(Map.class)) {
            return create(value.getAsList(ConfigValue::getAsIdentifier));
        }
        return fromSection(value.getAsSection());
    }

    private static @NotNull SourceItemColorDataSourceConfig fromSection(@NotNull ConfigSection section) {
        return create(section.getList("components", ConfigValue::getAsIdentifier));
    }

    private static @Nullable Color color(Item item, Key component) {
        ColorReader reader = SPECIAL_READERS.get(component);
        return reader != null ? reader.read(item) : numericColor(SourceItemComponentAccess.read(item, component));
    }

    private static @Nullable Color fireworkColor(Item item) {
        FireworkExplosion explosion = item.fireworkExplosion().orElse(null);
        return explosion == null || explosion.colors().isEmpty() ? null : new Color(explosion.colors().getInt(0));
    }

    private static @Nullable Color potionColor(Item item) {
        Tag value = SourceItemComponentAccess.read(item, DataComponentKeys.POTION_CONTENTS);
        if (value instanceof CompoundTag compound) {
            value = compound.get("custom_color");
        }
        return numericColor(value);
    }

    private static @Nullable Color numericColor(@Nullable Tag value) {
        return value instanceof NumericTag numeric ? new Color(numeric.getAsInt()) : null;
    }

    @Override
    public @NotNull FurnitureDataResolver<Color> bind(@NotNull Furniture furniture) {
        return () -> {
            Item sourceItem = furniture.sourceItem();
            if (sourceItem == null) {
                return null;
            }
            for (Key component : this.components) {
                Color color = color(sourceItem, component);
                if (color != null) {
                    return color;
                }
            }
            return null;
        };
    }

    @FunctionalInterface
    private interface ColorReader {
        @Nullable Color read(@NotNull Item item);
    }
}
