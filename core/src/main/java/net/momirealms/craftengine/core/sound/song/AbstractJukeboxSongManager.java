package net.momirealms.craftengine.core.sound.song;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJukeboxSongManager implements JukeboxSongManager {
    protected final Map<Key, JukeboxSong> songs = new HashMap<>();
    protected CraftEngine plugin;

    public AbstractJukeboxSongManager(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void unload() {
        this.songs.clear();
    }

    @Override
    public void delayedLoad() {
        if (!VersionHelper.isVersionNewerThan1_21()) return;
        this.registerSongs(this.songs);
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        if (this.songs.containsKey(id)) {
            this.plugin.logger().warn("Duplicate song id: " + id);
            return;
        }
        String sound = (String) section.get("sound");
        if (sound == null) {
            this.plugin.logger().warn(path, "No sound specified");
            return;
        }
        Component description = AdventureHelper.miniMessage(section.getOrDefault("description", "").toString());
        float length = MiscUtils.getAsFloat(section.get("length"));
        int comparatorOutput = MiscUtils.getAsInt(section.getOrDefault("comparator-output", 15));
        JukeboxSong song = new JukeboxSong(Key.of(sound), description, length, comparatorOutput, MiscUtils.getAsFloat(section.getOrDefault("range", 32f)));
        this.songs.put(id, song);
    }

    protected abstract void registerSongs(Map<Key, JukeboxSong> songs);
}
