package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.util.Key;

public class SoundUtils {

    private SoundUtils() {}

    public static Object toSoundType(BlockSounds sounds) throws ReflectiveOperationException {
        return Reflections.constructor$SoundType.newInstance(
            1f, 1f,
                getOrRegisterSoundEvent(sounds.breakSound()),
                getOrRegisterSoundEvent(sounds.stepSound()),
                getOrRegisterSoundEvent(sounds.placeSound()),
                getOrRegisterSoundEvent(sounds.hitSound()),
                getOrRegisterSoundEvent(sounds.fallSound())
        );
    }

    public static Object getOrRegisterSoundEvent(Key key) throws ReflectiveOperationException {
        return Reflections.method$SoundEvent$createVariableRangeEvent.invoke(null,
                Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, key.namespace(), key.value())
        );
    }
}
