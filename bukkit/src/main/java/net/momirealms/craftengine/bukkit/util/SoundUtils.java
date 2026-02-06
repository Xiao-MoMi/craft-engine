package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import org.bukkit.SoundCategory;

public final class SoundUtils {
    private SoundUtils() {}

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
