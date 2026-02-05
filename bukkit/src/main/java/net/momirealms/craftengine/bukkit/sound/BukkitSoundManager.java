package net.momirealms.craftengine.bukkit.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.JukeboxSong;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BukkitSoundManager extends AbstractSoundManager {

    public BukkitSoundManager(CraftEngine plugin) {
        super(plugin);
        for (Object soundEvent : (Iterable<?>) MBuiltInRegistries.SOUND_EVENT) {
            Object resourceLocation = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
            VANILLA_SOUND_EVENTS.add(KeyUtils.identifierToKey(resourceLocation));
        }
        this.registerSongs(this.loadLastRegisteredSongs());
    }

    @Override
    public void disable() {
        this.saveLastRegisteredSongs(super.songs);
        super.disable();
    }

    private void saveLastRegisteredSongs(Map<Key, JukeboxSong> songs) {
        if (songs == null || songs.isEmpty()) return;
        Path persistSongPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("jukebox-songs.json");
        try {
            Files.createDirectories(persistSongPath.getParent());
            JsonObject cache = new JsonObject();
            for (Map.Entry<Key, JukeboxSong> entry : songs.entrySet()) {
                JsonObject songJson = new JsonObject();
                JukeboxSong song = entry.getValue();
                songJson.addProperty("sound_event", song.sound().asString());
                songJson.add("description", AdventureHelper.componentToJsonElement(song.description()));
                songJson.addProperty("length_in_seconds", song.lengthInSeconds());
                songJson.addProperty("comparator_output", song.comparatorOutput());
                songJson.addProperty("range", song.range());
                cache.add(entry.getKey().asString(), songJson);
            }
            GsonHelper.writeJsonFile(cache, persistSongPath);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to save registered songs.", e);
        }
    }

    private Map<Key, JukeboxSong> loadLastRegisteredSongs() {
        Path persistSongPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("jukebox-songs.json");
        if (Files.exists(persistSongPath) && Files.isRegularFile(persistSongPath)) {
            try {
                Map<Key, JukeboxSong> songs = new HashMap<>();
                JsonObject cache = GsonHelper.readJsonFile(persistSongPath).getAsJsonObject();
                for (Map.Entry<String, JsonElement> songEntry : cache.entrySet()) {
                    Key id = Key.of(songEntry.getKey());
                    if (songEntry.getValue() instanceof JsonObject jo) {
                        songs.put(id, new JukeboxSong(
                                Key.of(jo.get("sound_event").getAsString()),
                                AdventureHelper.jsonElementToComponent(jo.get("description")),
                                jo.get("length_in_seconds").getAsFloat(),
                                jo.get("comparator_output").getAsInt(),
                                jo.get("range").getAsFloat()
                        ));
                    }
                }
                return songs;
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to load registered songs.", e);
            }
        }
        return Map.of();
    }

    @Override
    protected void registerSounds(Collection<Key> sounds) {
        if (sounds.isEmpty()) return;
        Object registry = MBuiltInRegistries.SOUND_EVENT;
        try {
            CoreReflections.field$MappedRegistry$frozen.set(registry, false);
            for (Key soundEventId : sounds) {
                Object resourceLocation = KeyUtils.toIdentifier(soundEventId);
                // 检查之前有没有注册过了
                Object soundEvent = FastNMS.INSTANCE.method$Registry$getValue(registry, resourceLocation);
                // 只有没注册才注册，否则会报错
                if (soundEvent == null) {
                    soundEvent = VersionHelper.isOrAbove1_21_2() ?
                            CoreReflections.constructor$SoundEvent.newInstance(resourceLocation, Optional.of(0)) :
                            CoreReflections.constructor$SoundEvent.newInstance(resourceLocation, 0, false);
                    Object holder = CoreReflections.method$Registry$registerForHolder.invoke(null, registry, resourceLocation, soundEvent);
                    CoreReflections.method$Holder$Reference$bindValue.invoke(holder, soundEvent);
                    CoreReflections.field$Holder$Reference$tags.set(holder, Set.of());
                    int id = FastNMS.INSTANCE.method$Registry$getId(registry, soundEvent);
                    super.customSoundsInRegistry.put(id, soundEventId);
                }
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to register jukebox songs.", e);
        } finally {
            try {
                CoreReflections.field$MappedRegistry$frozen.set(registry, true);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    @Override
    protected void registerSongs(Map<Key, JukeboxSong> songs) {
        if (songs.isEmpty()) return;
        Object registry = FastNMS.INSTANCE.method$RegistryAccess$lookupOrThrow(FastNMS.INSTANCE.registryAccess(), MRegistries.JUKEBOX_SONG);
        try {
            // 获取 JUKEBOX_SONG 注册表
            CoreReflections.field$MappedRegistry$frozen.set(registry, false);
            for (Map.Entry<Key, JukeboxSong> entry : songs.entrySet()) {
                Key id = entry.getKey();
                JukeboxSong jukeboxSong = entry.getValue();
                Object resourceLocation = KeyUtils.toIdentifier(id);
                Object soundId = KeyUtils.toIdentifier(jukeboxSong.sound());
                // 检查之前有没有注册过了
                Object song = FastNMS.INSTANCE.method$Registry$getValue(registry, resourceLocation);
                // 只有没注册才注册，否则会报错
                if (song == null) {
                    Object soundEvent = VersionHelper.isOrAbove1_21_2() ?
                            CoreReflections.constructor$SoundEvent.newInstance(soundId, Optional.of(jukeboxSong.range())) :
                            CoreReflections.constructor$SoundEvent.newInstance(soundId, jukeboxSong.range(), false);
                    Object soundHolder = CoreReflections.method$Holder$direct.invoke(null, soundEvent);
                    song = CoreReflections.constructor$JukeboxSong.newInstance(soundHolder, ComponentUtils.adventureToMinecraft(jukeboxSong.description()), jukeboxSong.lengthInSeconds(), jukeboxSong.comparatorOutput());
                    Object holder = CoreReflections.method$Registry$registerForHolder.invoke(null, registry, resourceLocation, song);
                    CoreReflections.method$Holder$Reference$bindValue.invoke(holder, song);
                    CoreReflections.field$Holder$Reference$tags.set(holder, Set.of());
                }
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to register jukebox songs.", e);
        } finally {
            try {
                CoreReflections.field$MappedRegistry$frozen.set(registry, true);
            } catch (ReflectiveOperationException ignored) {}
        }
    }
}
