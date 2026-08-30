package net.momirealms.craftengine.core.plugin.gui.category.source;

import java.util.ArrayList;
import java.util.List;

public final class CompositeCategorySource implements CategorySource {
    private final List<CategorySource> sources;

    public CompositeCategorySource(List<CategorySource> sources) {
        this.sources = List.copyOf(sources);
    }

    @Override
    public List<String> resolve(CategorySourceContext context) {
        List<String> resolved = new ArrayList<>();
        for (CategorySource source : this.sources) {
            resolved.addAll(source.resolve(context));
        }
        return resolved;
    }
}
