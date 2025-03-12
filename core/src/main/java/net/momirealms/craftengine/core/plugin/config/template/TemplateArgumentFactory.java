package net.momirealms.craftengine.core.plugin.config.template;

import java.util.Map;

public interface TemplateArgumentFactory {

    TemplateArgument create(Map<String, Object> arguments);
}
