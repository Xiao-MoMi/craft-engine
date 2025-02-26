package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;
import java.util.TreeSet;

public interface CategoryManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "categories";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    default int loadingSequence() {
        return LoadingSequence.CATEGORY;
    }

    void delayedLoad();

    TreeSet<Category> categories();

    Optional<Category> byId(Key key);
}
