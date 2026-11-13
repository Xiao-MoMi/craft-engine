package net.momirealms.craftengine.bukkit.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import org.bukkit.SoundCategory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SoundUtils {
    private SoundUtils() {}

    private static final Cache<Key, Object> SOUND_HOLDER_CACHE = Caffeine.newBuilder()
            .maximumSize(256)
            .build();
    private static final Map<String, BlockSounds> BLOCK_SOUNDS_CACHE = Collections.synchronizedMap(new HashMap<>());

    public static BlockSounds blockSoundsByName(String name) {
        return BLOCK_SOUNDS_CACHE.computeIfAbsent(name.toUpperCase(Locale.ROOT), key -> {
            Object soundType = SoundTypeProxy.INSTANCE.getByName(key);
            return soundType == null ? null : fromNMSSoundType(soundType);
        });
    }

    public static BlockSounds fromNMSSoundType(Object soundType) {
        float volume = SoundTypeProxy.INSTANCE.getVolume(soundType);
        float pitch = SoundTypeProxy.INSTANCE.getPitch(soundType);
        return new BlockSounds(
                fromNMSSoundEvent(SoundTypeProxy.INSTANCE.getBreakSound(soundType), volume, pitch * 0.8f),
                fromNMSSoundEvent(SoundTypeProxy.INSTANCE.getStepSound(soundType), volume * 0.15f, pitch),
                fromNMSSoundEvent(SoundTypeProxy.INSTANCE.getPlaceSound(soundType), volume, pitch * 0.8f),
                fromNMSSoundEvent(SoundTypeProxy.INSTANCE.getHitSound(soundType), volume * 0.5f, pitch * 0.5f),
                fromNMSSoundEvent(SoundTypeProxy.INSTANCE.getFallSound(soundType), volume * 0.5f, pitch * 0.75f)
        );
    }

    public static SoundData fromNMSSoundEvent(Object soundEvent, float volume, float pitch) {
        return new SoundData(KeyUtils.identifierToKey(SoundEventProxy.INSTANCE.getLocation(soundEvent)),
                SoundData.SoundValue.fixed(volume), SoundData.SoundValue.fixed(pitch));
    }

    // 高频发包路径用，避免每次 parse/create SoundEvent
    public static Object getOrCreateSoundHolder(Key key) {
        return SOUND_HOLDER_CACHE.get(key, k -> HolderProxy.INSTANCE.direct(SoundEventProxy.INSTANCE.createVariableRangeEvent(KeyUtils.toIdentifier(k))));
    }

    public static Enum<?> toNMS(SoundSource source) {
        return switch (source) {
            case BLOCK -> SoundSourceProxy.BLOCKS;
            case MUSIC -> SoundSourceProxy.MUSIC;
            case VOICE -> SoundSourceProxy.VOICE;
            case MASTER -> SoundSourceProxy.MASTER;
            case PLAYER -> SoundSourceProxy.PLAYERS;
            case RECORD -> SoundSourceProxy.RECORD;
            case AMBIENT -> SoundSourceProxy.AMBIENT;
            case HOSTILE -> SoundSourceProxy.HOSTILE;
            case NEUTRAL -> SoundSourceProxy.NEUTRAL;
            case WEATHER -> SoundSourceProxy.WEATHER;
            case UI -> SoundSourceProxy.UI;
        };
    }

    public static Object toNMSSoundType(BlockSounds sounds) {
        return SoundTypeProxy.INSTANCE.newInstance(
            1f, 1f,
                createSoundEvent(sounds.breakSound().id()),
                createSoundEvent(sounds.stepSound().id()),
                createSoundEvent(sounds.placeSound().id()),
                createSoundEvent(sounds.hitSound().id()),
                createSoundEvent(sounds.fallSound().id())
        );
    }

    public static Object createSoundEvent(Key key) {
        return SoundEventProxy.INSTANCE.createVariableRangeEvent(KeyUtils.toIdentifier(key));
    }

    public static SoundCategory toBukkit(SoundSource source) {
        return switch (source) {
            case BLOCK -> SoundCategory.BLOCKS;
            case MUSIC -> SoundCategory.MUSIC;
            case VOICE -> SoundCategory.VOICE;
            case MASTER -> SoundCategory.MASTER;
            case PLAYER -> SoundCategory.PLAYERS;
            case RECORD -> SoundCategory.RECORDS;
            case AMBIENT -> SoundCategory.AMBIENT;
            case HOSTILE -> SoundCategory.HOSTILE;
            case NEUTRAL -> SoundCategory.NEUTRAL;
            case WEATHER -> SoundCategory.WEATHER;
            case UI -> SoundCategory.UI;
        };
    }
}
