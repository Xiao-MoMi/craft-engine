/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
