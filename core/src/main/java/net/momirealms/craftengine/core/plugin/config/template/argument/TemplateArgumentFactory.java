package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface TemplateArgumentFactory<T extends TemplateArgument> {

    T create(ConfigSection section);
}
