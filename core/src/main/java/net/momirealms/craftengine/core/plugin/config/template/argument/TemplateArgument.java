package net.momirealms.craftengine.core.plugin.config.template.argument;

import java.util.Map;

public interface TemplateArgument {

    Object get(String node, Map<String, TemplateArgument> arguments);
}
