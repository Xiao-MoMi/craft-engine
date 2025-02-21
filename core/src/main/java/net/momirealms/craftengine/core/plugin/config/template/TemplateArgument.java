package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.util.Key;

import java.util.function.Supplier;

public interface TemplateArgument extends Supplier<Object> {

    Key type();
}
