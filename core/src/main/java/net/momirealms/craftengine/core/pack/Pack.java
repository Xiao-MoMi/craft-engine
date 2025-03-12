package net.momirealms.craftengine.core.pack;

import java.nio.file.Path;

/**
 * Represents a folder under the user's resources directory,
 * designed to simplify the installation of third-party resource packs.
 * <p>
 * The folder structure allows users to organize and manage
 * resource packs and configurations provided by external sources.
 * <p>
 * This class provides access to the resource pack folder
 * and configuration folder within the specified directory.
 */
public class Pack {
    private final Path folder;
    private final PackMeta meta;

    public Pack(Path folder, PackMeta meta) {
        this.folder = folder;
        this.meta = meta;
    }

    public String namespace() {
        return meta.namespace();
    }

    public PackMeta meta() {
        return meta;
    }

    public Path folder() {
        return folder;
    }

    /**
     * Returns the 'resourcepack' folder within the specified directory,
     * used for storing third-party resource packs.
     */
    public Path resourcePackFolder() {
        return folder.resolve("resourcepack");
    }

    /**
     * Returns the 'configuration' folder within the specified directory,
     * used for storing configuration files related to the resource packs.
     */
    public Path configurationFolder() {
        return folder.resolve("configuration");
    }
}
