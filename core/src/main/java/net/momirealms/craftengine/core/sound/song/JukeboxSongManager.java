package net.momirealms.craftengine.core.sound.song;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;

public interface JukeboxSongManager extends Reloadable, ConfigSectionParser {
    String[] CONFIG_SECTION_NAME = new String[] {"jukebox_songs", "song", "songs", "jukebox", "jukebox_song"};

    @Override
    default int loadingSequence() {
        return LoadingSequence.JUKEBOX_SONG;
    }

    @Override
    default String[] sectionId() {
        return CONFIG_SECTION_NAME;
    }

    void delayedLoad();
}
