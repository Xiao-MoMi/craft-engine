package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;

public interface PackManager extends Reloadable {

    @NotNull
    Collection<Pack> loadedPacks();

    boolean registerConfigSectionParser(ConfigSectionParser parser);

    boolean unregisterConfigSectionParser(String id);

    default void unregisterConfigSectionParser(ConfigSectionParser parser) {
        for (String id : parser.sectionId()) {
            unregisterConfigSectionParser(id);
        }
    }

    void delayedInit();

    void generateResourcePack();

    Path resourcePackPath();
}
