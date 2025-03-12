package net.momirealms.craftengine.bukkit.sound;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.song.JukeboxSongManager;

public class BukkitSoundManager extends AbstractSoundManager {

    public BukkitSoundManager(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected JukeboxSongManager createJukeboxSongManager() {
        return new BukkitJukeboxSongManager(super.plugin);
    }
}
