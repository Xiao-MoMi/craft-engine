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

    void delayedInit();

    void generateResourcePack();

    Path resourcePackPath();
}
