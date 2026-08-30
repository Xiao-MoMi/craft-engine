package net.momirealms.craftengine.core.plugin.gui.category.source;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.ArrayList;
import java.util.List;

public final class ListCategorySource implements CategorySource {
    private final List<String> entries;

    private ListCategorySource(List<String> entries) {
        this.entries = List.copyOf(entries);
    }

    static ListCategorySource fromConfig(ConfigSection section) {
        return new ListCategorySource(section.getStringList("list"));
    }

    @Override
    public List<String> resolve(CategorySourceContext context) {
        return new ArrayList<>(this.entries);
    }
}
