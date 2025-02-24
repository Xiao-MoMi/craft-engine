package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;

import java.util.Map;
import java.util.regex.Pattern;

public interface TemplateManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "templates";
    Pattern PATTERN = Pattern.compile("\\{[^{}]+}");
    String TEMPLATE = "template";
    String OVERRIDES = "overrides";
    String ARGUMENTS = "arguments";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    Map<String, Object> applyTemplates(Map<String, Object> input);

    default int loadingSequence() {
        return LoadingSequence.TEMPLATE;
    }
}
