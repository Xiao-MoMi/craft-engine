package net.momirealms.craftengine.bukkit.sound;

import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.song.AbstractJukeboxSongManager;
import net.momirealms.craftengine.core.sound.song.JukeboxSong;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BukkitJukeboxSongManager extends AbstractJukeboxSongManager {

    public BukkitJukeboxSongManager(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected void registerSongs(Map<Key, JukeboxSong> songs) {
        if (songs.isEmpty()) return;
        try {
            unfreezeRegistry();
            for (Map.Entry<Key, JukeboxSong> entry : songs.entrySet()) {
                Key id = entry.getKey();
                JukeboxSong jukeboxSong = entry.getValue();
                Object resourceLocation = Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, id.namespace(), id.value());
                Object soundId = Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, jukeboxSong.sound().namespace(), jukeboxSong.sound().value());
                Object song = Reflections.method$Registry$get.invoke(Reflections.instance$InternalRegistries$JUKEBOX_SONG, resourceLocation);

                Object soundEvent = VersionHelper.isVersionNewerThan1_21_2() ?
                        Reflections.constructor$SoundEvent.newInstance(soundId, Optional.of(jukeboxSong.range())) :
                        Reflections.constructor$SoundEvent.newInstance(soundId, jukeboxSong.range(), false);
                Object soundHolder = Reflections.method$Holder$direct.invoke(null, soundEvent);

                if (song == null) {
                    song = Reflections.constructor$JukeboxSong.newInstance(soundHolder, ComponentUtils.adventureToMinecraft(jukeboxSong.description()), jukeboxSong.lengthInSeconds(), jukeboxSong.comparatorOutput());
                    Object holder = Reflections.method$Registry$registerForHolder.invoke(null, Reflections.instance$InternalRegistries$JUKEBOX_SONG, resourceLocation, song);
                    Reflections.method$Holder$Reference$bindValue.invoke(holder, song);
                    Reflections.field$Holder$Reference$tags.set(holder, Set.of());
                }
            }
            freezeRegistry();
        } catch (Exception e) {
            plugin.logger().warn("Failed to register jukebox songs.", e);
        }
    }

    private void unfreezeRegistry() throws IllegalAccessException {
        Reflections.field$MappedRegistry$frozen.set(Reflections.instance$InternalRegistries$JUKEBOX_SONG, false);
    }

    private void freezeRegistry() throws IllegalAccessException {
        Reflections.field$MappedRegistry$frozen.set(Reflections.instance$InternalRegistries$JUKEBOX_SONG, true);
    }
}
