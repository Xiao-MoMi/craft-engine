package net.momirealms.craftengine.bukkit.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.JukeboxSong;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.JukeboxSongProxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BukkitSoundManager extends AbstractSoundManager {

    public BukkitSoundManager(CraftEngine plugin) {
        super(plugin);
        for (Object soundEvent : (Iterable<?>) BuiltInRegistriesProxy.SOUND_EVENT) {
            Object identifier = SoundEventProxy.INSTANCE.getLocation(soundEvent);
            VANILLA_SOUND_EVENTS.add(KeyUtils.identifierToKey(identifier));
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
        Object registry = BuiltInRegistriesProxy.SOUND_EVENT;
        try {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
            for (Key soundEventId : sounds) {
                Object identifier = KeyUtils.toIdentifier(soundEventId);
                // 检查之前有没有注册过了
                Object soundEvent = RegistryUtils.getRegistryValue(registry, identifier);
                // 只有没注册才注册，否则会报错
                if (soundEvent == null) {
                    soundEvent = SoundEventProxy.INSTANCE.create(identifier, Optional.of(0f));
                    Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, identifier, soundEvent);
                    HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, soundEvent);
                    HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
                    int id = RegistryProxy.INSTANCE.getId$0(registry, soundEvent);
                    super.customSoundsInRegistry.put(id, soundEventId);
                }
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to register jukebox songs.", e);
        } finally {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
        }
    }

    @Override
    protected void registerSongs(Map<Key, JukeboxSong> songs) {
        if (songs.isEmpty()) return;
        Object registryAccess = RegistryUtils.getRegistryAccess();
        Object registry = RegistryAccessProxy.INSTANCE.lookupOrThrow(registryAccess, RegistriesProxy.JUKEBOX_SONG);
        try {
            // 获取 JUKEBOX_SONG 注册表
            MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
            for (Map.Entry<Key, JukeboxSong> entry : songs.entrySet()) {
                Key id = entry.getKey();
                JukeboxSong jukeboxSong = entry.getValue();
                Object identifier = KeyUtils.toIdentifier(id);
                Object soundId = KeyUtils.toIdentifier(jukeboxSong.sound());
                // 检查之前有没有注册过了
                Object song = RegistryUtils.getRegistryValue(registry, identifier);
                // 只有没注册才注册，否则会报错
                if (song == null) {
                    Object soundEvent = SoundEventProxy.INSTANCE.create(soundId, Optional.of(jukeboxSong.range()));
                    Object soundHolder = HolderProxy.INSTANCE.direct(soundEvent);
                    song = JukeboxSongProxy.INSTANCE.newInstance(soundHolder, ComponentUtils.adventureToMinecraft(jukeboxSong.description()), jukeboxSong.lengthInSeconds(), jukeboxSong.comparatorOutput());
                    Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, identifier, song);
                    HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, song);
                    HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
                }
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to register jukebox songs.", e);
        } finally {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
        }
    }
}
