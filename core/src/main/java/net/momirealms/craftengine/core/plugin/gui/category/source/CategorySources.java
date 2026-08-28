package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.List;

public final class CategorySources {
    public static final CategorySourceType<AllItemsCategorySource> ALL_ITEMS = register(Key.ce("all_items"), AllItemsCategorySource::fromConfig);
    public static final CategorySourceType<ListCategorySource> LIST = register(Key.ce("list"), ListCategorySource::fromConfig);
    public static final CategorySourceType<RegexCategorySource> REGEX = register(Key.ce("regex"), RegexCategorySource::fromConfig);

    private CategorySources() {
    }

    public static <T extends CategorySource> CategorySourceType<T> register(Key key, CategorySourceFactory<T> factory) {
        CategorySourceType<T> type = new CategorySourceType<>(key, factory);
        ((WritableRegistry<CategorySourceType<? extends CategorySource>>) BuiltInRegistries.CATEGORY_SOURCE_TYPE)
                .register(ResourceKey.create(Registries.CATEGORY_SOURCE_TYPE.location(), key), type);
        return type;
    }

    public static CategorySource fromConfig(ConfigValue value) {
        if (value.is(List.class)) {
            return new CompositeCategorySource(value.getAsList(CategorySources::fromConfig));
        }
        return fromConfig(value.getAsSection());
    }

    public static CategorySource fromConfig(ConfigSection section) {
        String typeName = section.getNonEmptyString("type");
        Key key = Key.ce(typeName);
        CategorySourceType<? extends CategorySource> type = BuiltInRegistries.CATEGORY_SOURCE_TYPE.getValue(key);
        if (type == null) {
            throw new KnownResourceException("category.source.unknown_type", section.assemblePath("type"), typeName);
        }
        return type.factory().create(section);
    }
}
