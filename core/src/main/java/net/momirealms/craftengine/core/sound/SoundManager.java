package net.momirealms.craftengine.core.sound;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public interface SoundManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "sounds";

    Map<Key, SoundEvent> sounds();

    @Override
    default int loadingSequence() {
        return LoadingSequence.SOUND;
    }

    @Override
    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }
}
