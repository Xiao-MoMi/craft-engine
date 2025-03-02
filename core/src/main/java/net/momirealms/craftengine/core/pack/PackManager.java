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

    default boolean unregisterConfigSectionParser(ConfigSectionParser parser) {
        return this.unregisterConfigSectionParser(parser.sectionId());
    }

    void generateResourcePack();

    Path resourcePackPath();
}
