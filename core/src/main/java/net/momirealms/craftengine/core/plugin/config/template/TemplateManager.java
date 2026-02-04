package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;

public interface TemplateManager extends Manageable {

    TemplateManager INSTANCE = new TemplateManagerImpl();

    ConfigParser parser();

    Object applyTemplates(Key id, Object input);

    Object applyTemplates(Object input);
}
