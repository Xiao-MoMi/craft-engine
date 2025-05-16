package net.momirealms.craftengine.core.sound;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractSoundManager implements SoundManager {
    protected static final Set<Key> VANILLA_SOUND_EVENTS = new HashSet<>();
    protected final CraftEngine plugin;
    protected final Map<Key, SoundEvent> byId = new HashMap<>();
    protected final Map<String, List<SoundEvent>> byNamespace = new HashMap<>();
    protected final Map<Key, JukeboxSong> songs = new HashMap<>();
    protected final SoundParser soundParser;
    protected final SongParser songParser;

    public AbstractSoundManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.soundParser = new SoundParser();
        this.songParser = new SongParser();
    }

    @Override
    public boolean isVanillaSoundEvent(Key key) {
        return VANILLA_SOUND_EVENTS.contains(key);
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] { this.soundParser, this.songParser };
    }

    @Override
    public void unload() {
        this.byId.clear();
        this.byNamespace.clear();
    }

    @Override
    public void runDelayedSyncTasks() {
        if (!VersionHelper.isOrAbove1_21()) return;
        this.registerSongs(this.songs);
    }

    @Override
    public Map<Key, SoundEvent> sounds() {
        return Collections.unmodifiableMap(this.byId);
    }

    public Map<String, List<SoundEvent>> soundsByNamespace() {
        return Collections.unmodifiableMap(this.byNamespace);
    }

    protected abstract void registerSongs(Map<Key, JukeboxSong> songs);

    public class SongParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"jukebox_songs", "song", "songs", "jukebox", "jukebox_song"};

        @Override
        public int loadingSequence() {
            return LoadingSequence.JUKEBOX_SONG;
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (AbstractSoundManager.this.songs.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.jukebox_song.duplicate");
            }
            String sound = ResourceConfigUtils.requireNonEmptyStringOrThrow(section.get("sound"), "warning.config.jukebox_song.missing_sound");
            Component description = AdventureHelper.miniMessage().deserialize(section.getOrDefault("description", "").toString());
            float length = ResourceConfigUtils.getAsFloat(section.get("length"), "length");
            int comparatorOutput = ResourceConfigUtils.getAsInt(section.getOrDefault("comparator-output", 15), "comparator-output");
            JukeboxSong song = new JukeboxSong(Key.of(sound), description, length, comparatorOutput, ResourceConfigUtils.getAsFloat(section.getOrDefault("range", 32f), "range"));
            AbstractSoundManager.this.songs.put(id, song);
        }
    }

    public class SoundParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"sounds", "sound"};

        @Override
        public int loadingSequence() {
            return LoadingSequence.SOUND;
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (AbstractSoundManager.this.byId.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.sound.duplicate");
            }
            boolean replace = (boolean) section.getOrDefault("replace", false);
            String subtitle = (String) section.get("subtitle");
            List<?> soundList = (List<?>) ResourceConfigUtils.requireNonNullOrThrow(section.get("sounds"), "warning.config.sound.missing_sounds");
            List<Sound> sounds = new ArrayList<>();
            for (Object sound : soundList) {
                if (sound instanceof String soundPath) {
                    sounds.add(Sound.path(soundPath));
                } else if (sound instanceof Map<?,?> map) {
                    sounds.add(Sound.SoundFile.fromMap(MiscUtils.castToMap(map, false)));
                }
            }
            SoundEvent event = new SoundEvent(id, replace, subtitle, sounds);
            AbstractSoundManager.this.byId.put(id, event);
            AbstractSoundManager.this.byNamespace.computeIfAbsent(id.namespace(), k -> new ArrayList<>()).add(event);
        }
    }
}
