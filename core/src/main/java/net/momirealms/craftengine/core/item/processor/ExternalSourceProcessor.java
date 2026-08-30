package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.compatibility.ItemSource;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.LazyReference;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

public final class ExternalSourceProcessor implements ItemProcessor {
    public static final ItemProcessorFactory<ExternalSourceProcessor> FACTORY = new Factory();
    private static final ThreadLocal<Set<Dependency>> BUILD_STACK = ThreadLocal.withInitial(LinkedHashSet::new);
    private final String id;
    private final LazyReference<ItemSource> provider;
    private final boolean replace;

    public ExternalSourceProcessor(String id, boolean replace, LazyReference<ItemSource> provider) {
        this.id = id;
        this.provider = provider;
        this.replace = replace;
    }

    public String id() {
        return id;
    }

    @Override
    public void apply(ItemBuildContext context) {
        ItemSource provider = this.provider.get();
        if (provider == null) return;

        Dependency dependency = new Dependency(provider.plugin(), this.id);
        Set<Dependency> buildStack = BUILD_STACK.get();

        if (buildStack.contains(dependency)) {
            StringJoiner dependencyChain = new StringJoiner(" -> ");
            buildStack.forEach(element -> dependencyChain.add(element.asString()));
            dependencyChain.add(dependency.asString());
            CraftEngine.instance().logger().warn(
                    "Failed to build '" + this.id + "' from plugin '" + provider.plugin() + "' due to dependency loop: " + dependencyChain
            );
            return;
        }

        buildStack.add(dependency);
        try {
            Item another = provider.build(this.id, context);
            if (another == null) {
                CraftEngine.instance().logger().warn("'" + this.id + "' could not be found in " + provider.plugin());
                return;
            }
            if (this.replace) {
                context.setItem(another);
            } else {
                context.item().merge(another);
            }
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to build item '" + this.id + "' from plugin '" + provider.plugin() + "'", e);
        } finally {
            buildStack.remove(dependency);
            BUILD_STACK.remove();
        }
    }

    private static class Factory implements ItemProcessorFactory<ExternalSourceProcessor> {
        private static final String[] PLUGIN = ConfigKeys.of("plugin|source");

        @Override
        public ExternalSourceProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            String plugin = section.getNonNullString(PLUGIN);
            String id = section.getNonNullString("id");
            return new ExternalSourceProcessor(id, section.getBoolean("replace"), LazyReference.untilNotNull(() -> {
                ItemSource itemSource = CraftEngine.instance().compatibilityManager().getItemSource(plugin.toLowerCase(Locale.ENGLISH));
                if (itemSource == null) {
                    CraftEngine.instance().logger().warn("Item source '" + plugin + "' not found for item '" + id + "'");
                }
                return itemSource;
            }));
        }
    }

    private record Dependency(String source, String id) {

        public @NotNull String asString() {
            return this.source + "[id=" + this.id + "]";
        }
    }
}
