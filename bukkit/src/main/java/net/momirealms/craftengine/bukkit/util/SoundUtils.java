package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.SoundCategory;

public final class SoundUtils {

    private SoundUtils() {}

    public static Object toSoundType(BlockSounds sounds) throws ReflectiveOperationException {
        return CoreReflections.constructor$SoundType.newInstance(
            1f, 1f,
                getOrRegisterSoundEvent(sounds.breakSound().id()),
                getOrRegisterSoundEvent(sounds.stepSound().id()),
                getOrRegisterSoundEvent(sounds.placeSound().id()),
                getOrRegisterSoundEvent(sounds.hitSound().id()),
                getOrRegisterSoundEvent(sounds.fallSound().id())
        );
    }

    public static Object getOrRegisterSoundEvent(Key key) throws ReflectiveOperationException {
        return CoreReflections.method$SoundEvent$createVariableRangeEvent.invoke(null, KeyUtils.toResourceLocation(key));
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
