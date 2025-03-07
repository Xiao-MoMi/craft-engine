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
