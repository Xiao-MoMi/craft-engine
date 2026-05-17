package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;

public final class BlockSounds {
    /*
    Fall 0.5 0.75
    Place 1 0.8
    Step 0.15 1
    Hit 0.5 0.5
    Break 1 0.8
    Land 0.3 1
    Destroy 1 1
     */
    public static final SoundData EMPTY_SOUND = new SoundData(Key.of("minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1);
    public static final BlockSounds EMPTY = new BlockSounds(SoundData.EMPTY, SoundData.EMPTY, SoundData.EMPTY, SoundData.EMPTY, SoundData.EMPTY);

    private final SoundData breakSound;
    private final SoundData stepSound;
    private final SoundData placeSound;
    private final SoundData hitSound;
    private final SoundData fallSound;

    public BlockSounds(SoundData breakSound, SoundData stepSound, SoundData placeSound, SoundData hitSound, SoundData fallSound) {
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    public static BlockSounds fromConfig(ConfigSection section) {
        if (section == null) return EMPTY;
        return new BlockSounds(
                section.getValue("break", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_0_8), EMPTY_SOUND),
                section.getValue("step", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_15, SoundData.SoundValue.FIXED_1), EMPTY_SOUND),
                section.getValue("place", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_0_8), EMPTY_SOUND),
                section.getValue("hit", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.FIXED_0_5), EMPTY_SOUND),
                section.getValue("fall", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.FIXED_0_75), EMPTY_SOUND)
        );
    }

    public SoundData breakSound() {
        return breakSound;
    }

    public SoundData stepSound() {
        return stepSound;
    }

    public SoundData placeSound() {
        return placeSound;
    }

    public SoundData hitSound() {
        return hitSound;
    }

    public SoundData fallSound() {
        return fallSound;
    }
}
