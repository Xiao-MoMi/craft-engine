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
