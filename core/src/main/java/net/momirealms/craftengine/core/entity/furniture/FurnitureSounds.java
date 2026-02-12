package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FurnitureSounds {
    public static final SoundData EMPTY_SOUND = new SoundData(Key.of("minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1);
    public static final FurnitureSounds EMPTY = new FurnitureSounds(EMPTY_SOUND, EMPTY_SOUND, EMPTY_SOUND);

    private final SoundData breakSound;
    private final SoundData placeSound;
    private final SoundData hitSound;

    public FurnitureSounds(SoundData breakSound, SoundData placeSound, SoundData hitSound) {
        this.breakSound = breakSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
    }

    public static FurnitureSounds fromMap(Map<String, Object> map) {
        if (map == null) return EMPTY;
        return new FurnitureSounds(
                SoundData.create(map.getOrDefault("break", "minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_0_8),
                SoundData.create(map.getOrDefault("place", "minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_0_8),
                SoundData.create(map.getOrDefault("hit", "minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_0_5)
        );
    }

    public SoundData breakSound() {
        return this.breakSound;
    }

    public SoundData placeSound() {
        return this.placeSound;
    }

    public SoundData hitSound() {
        return this.hitSound;
    }
}
