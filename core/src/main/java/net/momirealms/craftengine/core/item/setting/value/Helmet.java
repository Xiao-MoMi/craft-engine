package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.sound.SoundData;

public final class Helmet {
    private final SoundData equipSound;

    public Helmet(SoundData equipSound) {
        this.equipSound = equipSound;
    }

    public SoundData equipSound() {
        return equipSound;
    }
}
