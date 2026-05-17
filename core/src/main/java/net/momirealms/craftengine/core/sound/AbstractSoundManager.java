package net.momirealms.craftengine.core.sound;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractSoundManager implements SoundManager {
    protected static final Set<Key> VANILLA_SOUND_EVENTS = new HashSet<>();
    protected final CraftEngine plugin;
    protected final Map<Key, SoundEvent> byId = new HashMap<>();
    protected final Map<String, List<SoundEvent>> byNamespace = new HashMap<>();
    protected final Map<Key, JukeboxSong> songs = new HashMap<>();
    protected final ConfigParser soundParser;
    protected final ConfigParser songParser;
//    protected final Map<Integer, Key> customSoundsInRegistry = new HashMap<>();
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
        if (!VersionHelper.isOrAbove1_21) return;
//        this.registerSounds(this.byId.keySet());
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

    private final class JukeboxSongParser extends IdSectionConfigParser {
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

        private static final String[] COMPARATOR = new String[] {"comparator-output", "comparator_output"};

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            Key sound = section.getNonNullIdentifier("sound");
            Component description = AdventureHelper.miniMessage().deserialize(section.getString("description", ""));
            float length = section.getNonNullFloat("length");
            float range = section.getFloat("range", 32f);
            int comparatorOutput = section.getInt(COMPARATOR, 15);
            JukeboxSong song = new JukeboxSong(sound, description, length, comparatorOutput, range);
            AbstractSoundManager.this.songs.put(id, song);
        }
    }

    private final class SoundParser extends IdSectionConfigParser {
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

        private static final String[] SOUNDS = new String[] {"sounds", "sound"};

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            boolean replace = section.getBoolean("replace");
            String subtitle = section.getString("subtitle");
            List<Sound> sounds = section.getList(SOUNDS, v -> {
                if (v.is(Map.class)) {
                    return Sound.SoundFile.fromConfig(v.getAsSection());
                } else {
                    return Sound.path(v.getAsString());
                }
            });
            SoundEvent event = new SoundEvent(id, replace, subtitle, sounds);
            AbstractSoundManager.this.byId.put(id, event);
            AbstractSoundManager.this.byNamespace.computeIfAbsent(id.namespace(), k -> new ArrayList<>()).add(event);
        }
    }
}
