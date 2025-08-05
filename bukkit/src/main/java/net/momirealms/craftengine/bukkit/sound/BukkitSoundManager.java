package net.momirealms.craftengine.bukkit.sound;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.AbstractSoundManager;
import net.momirealms.craftengine.core.sound.JukeboxSong;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BukkitSoundManager extends AbstractSoundManager {

    public BukkitSoundManager(CraftEngine plugin) {
        super(plugin);
        for (Object soundEvent : (Iterable<?>) MBuiltInRegistries.SOUND_EVENT) {
            Object resourceLocation = FastNMS.INSTANCE.field$SoundEvent$location(soundEvent);
            VANILLA_SOUND_EVENTS.add(KeyUtils.resourceLocationToKey(resourceLocation));
        }
    }

    @Override
    protected void registerSounds(Collection<Key> sounds) {
        if (sounds.isEmpty()) return;
        Object registry = MBuiltInRegistries.SOUND_EVENT;
        try {
            CoreReflections.field$MappedRegistry$frozen.set(registry, false);
            for (Key soundEventId : sounds) {
                Object resourceLocation = KeyUtils.toResourceLocation(soundEventId);
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
                Object resourceLocation = KeyUtils.toResourceLocation(id);
                Object soundId = KeyUtils.toResourceLocation(jukeboxSong.sound());
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
