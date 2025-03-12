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

package net.momirealms.craftengine.core.sound;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.sound.song.JukeboxSongManager;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public interface SoundManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "sounds";

    void delayedLoad();

    Map<Key, SoundEvent> sounds();

    JukeboxSongManager jukeboxSongManager();

    @Override
    default int loadingSequence() {
        return LoadingSequence.SOUND;
    }

    @Override
    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }
}
