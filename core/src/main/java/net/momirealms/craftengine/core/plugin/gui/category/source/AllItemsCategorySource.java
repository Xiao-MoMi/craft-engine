package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class AllItemsCategorySource implements CategorySource {
    private static final String[] IGNORE_VANILLA = ConfigKeys.of("ignore_vanilla");
    private final boolean ignoreVanilla;

    private AllItemsCategorySource(boolean ignoreVanilla) {
        this.ignoreVanilla = ignoreVanilla;
    }

    static AllItemsCategorySource fromConfig(ConfigSection section) {
        return new AllItemsCategorySource(section.getBoolean(IGNORE_VANILLA, true));
    }

    @Override
    public List<String> resolve(CategorySourceContext context) {
        return context.itemIds().stream()
                .filter(itemId -> !this.ignoreVanilla || !context.isVanillaItem(itemId))
                .map(Key::asString)
                .toList();
    }
}
