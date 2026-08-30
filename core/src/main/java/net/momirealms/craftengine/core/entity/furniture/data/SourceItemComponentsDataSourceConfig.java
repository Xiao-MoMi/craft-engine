package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class SourceItemComponentsDataSourceConfig implements FurnitureDataSourceConfig<ItemPatch> {
    private final List<Key> components;

    private SourceItemComponentsDataSourceConfig(List<Key> components) {
        this.components = List.copyOf(components);
    }

    @NotNull
    public static SourceItemComponentsDataSourceConfig create(@NotNull List<Key> components) {
        return new SourceItemComponentsDataSourceConfig(components);
    }

    @NotNull
    public static SourceItemComponentsDataSourceConfig fromConfig(@NotNull ConfigValue value) {
        if (!value.is(Map.class)) {
            return create(value.getAsList(ConfigValue::getAsIdentifier));
        }
        return fromSection(value.getAsSection());
    }

    @NotNull
    private static SourceItemComponentsDataSourceConfig fromSection(@NotNull ConfigSection section) {
        return create(section.getList("components", ConfigValue::getAsIdentifier));
    }

    @Override
    public @NotNull FurnitureDataResolver<ItemPatch> bind(@NotNull Furniture furniture) {
        return () -> {
            Item sourceItem = furniture.sourceItem();
            if (sourceItem == null) {
                return null;
            }
            return target -> apply(sourceItem, target);
        };
    }

    private void apply(Item source, Item target) {
        for (Key component : this.components) {
            SourceItemComponentAccess.copy(source, target, component);
        }
    }
}
