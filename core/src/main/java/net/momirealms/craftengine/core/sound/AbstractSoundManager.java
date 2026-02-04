package net.momirealms.craftengine.core.sound;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import org.incendo.cloud.suggestion.Suggestion;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractSoundManager implements SoundManager {
    protected static final Set<Key> VANILLA_SOUND_EVENTS = new HashSet<>();
    protected final CraftEngine plugin;
    protected final Map<Key, SoundEvent> byId = new HashMap<>();
    protected final Map<String, List<SoundEvent>> byNamespace = new HashMap<>();
    protected final Map<Key, JukeboxSong> songs = new HashMap<>();
    protected final SoundParser soundParser;
    protected final JukeboxSongParser songParser;
    protected final Map<Integer, Key> customSoundsInRegistry = new HashMap<>();
    protected final List<Suggestion> soundSuggestions = new ArrayList<>();

    public AbstractSoundManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.soundParser = new SoundParser();
        this.songParser = new JukeboxSongParser();
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
        this.songs.clear();
        this.soundSuggestions.clear();
    }

    @Override
    public void delayedLoad() {
        for (Key key : VANILLA_SOUND_EVENTS) {
            this.soundSuggestions.add(Suggestion.suggestion(key.asString()));
        }
        for (Key key : this.byId.keySet()) {
            this.soundSuggestions.add(Suggestion.suggestion(key.asString()));
        }
    }

    @Override
    public List<Suggestion> cachedSoundSuggestions() {
        return this.soundSuggestions;
    }

    @Override
    public void runDelayedSyncTasks() {
        if (!VersionHelper.isOrAbove1_21()) return;
        // 问题是会踢客户端
        // this.registerSounds(this.byId.keySet());
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

    protected abstract void registerSounds(Collection<Key> sounds);

    public final class JukeboxSongParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"jukebox-songs", "jukebox-song", "jukebox_songs", "jukebox_song"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return AbstractSoundManager.this.songs.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.JUKEBOX_SONG;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.SOUND);
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
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

    public final class SoundParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"sounds", "sound"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return AbstractSoundManager.this.byId.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.SOUND;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (AbstractSoundManager.this.byId.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.sound.duplicate");
            }
            boolean replace = ResourceConfigUtils.getAsBoolean(section.getOrDefault("replace", false), "replace");
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
