package net.momirealms.craftengine.core.plugin.gui.category.source;

import java.util.List;

public interface CategorySource {

    List<String> resolve(CategorySourceContext context);
}
