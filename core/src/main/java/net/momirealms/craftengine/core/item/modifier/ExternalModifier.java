package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ExternalItemSource;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Locale;
import java.util.Map;

public class ExternalModifier<I> implements ItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final String id;
    private final ExternalItemSource<I> provider;

    public ExternalModifier(String id, ExternalItemSource<I> provider) {
        this.id = id;
        this.provider = provider;
    }

    public String id() {
        return id;
    }

    public ExternalItemSource<I> source() {
        return provider;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.EXTERNAL;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        I another = this.provider.build(id, context);
        if (another == null) {
            CraftEngine.instance().logger().warn("'" + id + "' could not be found in " + provider.plugin());
            return item;
        }
        Item<I> anotherWrapped = (Item<I>) CraftEngine.instance().itemManager().wrap(another);
        item.merge(anotherWrapped);
        return item;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @SuppressWarnings("unchecked")
        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "external");
            String plugin = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(data, "plugin", "source"), "warning.config.item.data.external.missing_source");
            String id = ResourceConfigUtils.requireNonEmptyStringOrThrow(data.get("id"), "warning.config.item.data.external.missing_id");
            ExternalItemSource<I> provider = (ExternalItemSource<I>) CraftEngine.instance().itemManager().getExternalItemSource(plugin.toLowerCase(Locale.ENGLISH));
            return new ExternalModifier<>(id, ResourceConfigUtils.requireNonNullOrThrow(provider, () -> new LocalizedResourceConfigException("warning.config.item.data.external.invalid_source", plugin)));
        }
    }
}
