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

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.song.JukeboxSongManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractSoundManager implements SoundManager {
    protected final CraftEngine plugin;
    protected final Map<Key, SoundEvent> byId;
    protected final Map<String, List<SoundEvent>> byNamespace;
    protected final JukeboxSongManager jukeboxSongManager;

    public AbstractSoundManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.jukeboxSongManager = createJukeboxSongManager();
        this.byId = new HashMap<>();
        this.byNamespace = new HashMap<>();
    }

    protected abstract JukeboxSongManager createJukeboxSongManager();

    @Override
    public void unload() {
        this.byId.clear();
        this.byNamespace.clear();
        this.jukeboxSongManager.unload();
    }

    @Override
    public void load() {
        this.jukeboxSongManager.load();
    }

    @Override
    public void delayedLoad() {
        this.jukeboxSongManager.delayedLoad();
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        if (this.byId.containsKey(id)) {
            this.plugin.logger().warn(path, "Sound " + id + " already exists");
            return;
        }
        boolean replace = (boolean) section.getOrDefault("replace", false);
        String subtitle = (String) section.get("subtitle");
        List<?> soundList = (List<?>) section.get("sounds");
        List<Sound> sounds = new ArrayList<>();
        for (Object sound : soundList) {
            if (sound instanceof String soundPath) {
                sounds.add(Sound.path(soundPath));
            } else if (sound instanceof Map<?,?> map) {
                sounds.add(Sound.SoundFile.fromMap(MiscUtils.castToMap(map, false)));
            }
        }
        SoundEvent event = new SoundEvent(id, replace, subtitle, sounds);
        this.byId.put(id, event);
        this.byNamespace.computeIfAbsent(id.namespace(), k -> new ArrayList<>()).add(event);
    }

    @Override
    public Map<Key, SoundEvent> sounds() {
        return Collections.unmodifiableMap(this.byId);
    }

    public Map<String, List<SoundEvent>> soundsByNamespace() {
        return Collections.unmodifiableMap(this.byNamespace);
    }

    @Override
    public JukeboxSongManager jukeboxSongManager() {
        return this.jukeboxSongManager;
    }
}
