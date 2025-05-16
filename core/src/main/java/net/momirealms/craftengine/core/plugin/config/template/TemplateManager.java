package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;

import java.util.Map;
import java.util.regex.Pattern;

public interface TemplateManager extends Manageable {
    Pattern ARGUMENT_PATTERN = Pattern.compile("\\{[^{}]+}");
    String LEFT_BRACKET = "{";
    String RIGHT_BRACKET = "}";
    String TEMPLATE = "template";
    String OVERRIDES = "overrides";
    String ARGUMENTS = "arguments";

    ConfigParser parser();

    Map<String, Object> applyTemplates(Map<String, Object> input);
}
