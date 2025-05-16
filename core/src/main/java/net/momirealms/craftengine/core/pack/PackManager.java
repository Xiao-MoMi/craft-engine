package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public interface PackManager extends Manageable {

    void loadResources(boolean recipe);

    void initCachedAssets();

    @NotNull
    Collection<Pack> loadedPacks();

    boolean registerConfigSectionParser(ConfigParser parser);

    default void registerConfigSectionParsers(ConfigParser[] parsers) {
        for (ConfigParser parser : parsers) {
            registerConfigSectionParser(parser);
        }
    }

    boolean unregisterConfigSectionParser(String id);

    default void unregisterConfigSectionParser(ConfigParser parser) {
        for (String id : parser.sectionId()) {
            unregisterConfigSectionParser(id);
        }
    }

    void generateResourcePack() throws IOException;

    Path resourcePackPath();

    ResourcePackHost resourcePackHost();

    void uploadResourcePack();

    void sendResourcePack(Player player);
}
