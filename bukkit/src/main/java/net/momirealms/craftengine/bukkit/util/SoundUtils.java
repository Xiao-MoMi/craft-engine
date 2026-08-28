package net.momirealms.craftengine.bukkit.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import org.bukkit.SoundCategory;

public final class SoundUtils {
    private SoundUtils() {}

    private static final Cache<Key, Object> SOUND_HOLDER_CACHE = Caffeine.newBuilder()
            .maximumSize(256)
            .build();

    // 高频发包路径用，避免每次 parse/create SoundEvent
    public static Object getOrCreateSoundHolder(Key key) {
        return SOUND_HOLDER_CACHE.get(key, k ->
                HolderProxy.INSTANCE.direct(SoundEventProxy.INSTANCE.createVariableRangeEvent(KeyUtils.toIdentifier(k))));
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
