package net.momirealms.craftengine.core.item.modifier;

import cn.gtemc.itembridge.api.ItemBridge;
import cn.gtemc.itembridge.api.Provider;
import cn.gtemc.itembridge.api.context.BuildContext;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ExternalModifier<I> implements ItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private static final ThreadLocal<Set<Dependency>> BUILD_STACK = ThreadLocal.withInitial(LinkedHashSet::new);
    private final String id;
    private final LazyReference<Provider<I, Object>> provider;

    public ExternalModifier(String id, LazyReference<Provider<I, Object>> provider) {
        this.id = id;
        this.provider = provider;
    }

    public String id() {
        return id;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.EXTERNAL;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        Provider<I, Object> provider = this.provider.get();
        if (provider == null) return item;

        Dependency dependency = new Dependency(provider.plugin(), this.id);
        Set<Dependency> buildStack = BUILD_STACK.get();

        if (buildStack.contains(dependency)) {
            StringJoiner dependencyChain = new StringJoiner(" -> ");
            buildStack.forEach(element -> dependencyChain.add(element.asString()));
            dependencyChain.add(dependency.asString());
            CraftEngine.instance().logger().warn(
                    "Failed to build '" + this.id + "' from plugin '" + provider.plugin() + "' due to dependency loop: " + dependencyChain
            );
            return item;
        }

        buildStack.add(dependency);
        try {
            Player player = context.player();
            I another = provider.buildOrNull(this.id, player != null ? player.platformPlayer() : null, BuildContext.empty());
            if (another == null) {
                CraftEngine.instance().logger().warn("'" + this.id + "' could not be found in " + provider.plugin());
                return item;
            }
            Item<I> anotherWrapped = (Item<I>) CraftEngine.instance().itemManager().wrap(another);
            item.merge(anotherWrapped);
            return item;
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to build item '" + this.id + "' from plugin '" + provider.plugin() + "'", e);
            return item;
        } finally {
            buildStack.remove(dependency);
            BUILD_STACK.remove();
        }
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "external");
            String plugin = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(data, "plugin", "source"), "warning.config.item.data.external.missing_source");
            String id = ResourceConfigUtils.requireNonEmptyStringOrThrow(data.get("id"), "warning.config.item.data.external.missing_id");
            return new ExternalModifier<>(id, LazyReference.lazyReference(() -> {
                ItemBridge<I, Object> itemBridge = CraftEngine.instance().compatibilityManager().itemBridge();
                Optional<Provider<I, Object>> itemSource = itemBridge.provider(plugin.toLowerCase(Locale.ROOT));
                if (itemSource.isEmpty()) {
                    CraftEngine.instance().logger().warn("Item source '" + plugin + "' not found for item '" + id + "'");
                }
                return itemSource.orElse(null);
            }));
        }
    }

    private record Dependency(String source, String id) {

        public @NotNull String asString() {
            return this.source + "[id=" + this.id + "]";
        }
    }
}
