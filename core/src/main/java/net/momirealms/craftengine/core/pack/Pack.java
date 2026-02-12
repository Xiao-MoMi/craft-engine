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
public final class Pack {
    private final Path folder;
    private final PackMeta meta;
    private final boolean enabled;
    private final String[] subpacks;

    public Pack(Path folder, PackMeta meta, boolean enabled, String[] subpacks) {
        this.folder = folder;
        this.meta = meta;
        this.enabled = enabled;
        this.subpacks = subpacks;
    }

    public String name() {
        return this.folder.getFileName().toString();
    }

    public String namespace() {
        return this.meta.namespace();
    }

    public boolean enabled() {
        return this.enabled;
    }

    public PackMeta meta() {
        return this.meta;
    }

    public Path folder() {
        return this.folder;
    }

    /**
     * Returns the 'resourcepack' folder within the specified directory,
     * used for storing third-party resource packs.
     */
    public Path resourcePackFolder() {
        return this.folder.resolve("resourcepack");
    }

    /**
     * Returns the 'configuration' folder within the specified directory,
     * used for storing configuration files related to the resource packs.
     */
    public Path configurationFolder() {
        return this.folder.resolve("configuration");
    }

    public Path[] resourcePackFolders() {
        if (this.subpacks.length == 0) return new Path[] {resourcePackFolder()};
        Path[] folders = new Path[1 + this.subpacks.length];
        folders[0] = resourcePackFolder();
        for (int i = 1; i <= this.subpacks.length; i++) {
            folders[i] = this.folder.resolve("subpacks").resolve(this.subpacks[i - 1]).resolve("resourcepack");
        }
        return folders;
    }

    public Path[] configurationFolders() {
        if (this.subpacks.length == 0) return new Path[] {configurationFolder()};
        Path[] folders = new Path[1 + this.subpacks.length];
        folders[0] = configurationFolder();
        for (int i = 1; i <= this.subpacks.length; i++) {
            folders[i] = this.folder.resolve("subpacks").resolve(this.subpacks[i - 1]).resolve("configuration");
        }
        return folders;
    }
}
