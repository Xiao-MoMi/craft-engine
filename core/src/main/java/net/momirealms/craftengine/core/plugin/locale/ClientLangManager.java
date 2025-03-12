package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;

import java.util.Map;

public interface ClientLangManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "lang";

    Map<String, I18NData> langData();

    @Override
    default int loadingSequence() {
        return LoadingSequence.LANG;
    }

    @Override
    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }
}
