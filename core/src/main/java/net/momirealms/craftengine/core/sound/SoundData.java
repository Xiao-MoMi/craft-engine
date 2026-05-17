package net.momirealms.craftengine.core.sound;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SoundData(Key id, SoundValue volume, SoundValue pitch) {
    public static final SoundData EMPTY = new SoundData(Key.of("minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1);

    public static SoundData of(Key id, SoundValue volume, SoundValue pitch) {
        return new SoundData(id, volume, pitch);
    }

    public static SoundData fromConfig(ConfigValue value, SoundData.SoundValue volume, SoundData.SoundValue pitch) {
        Key soundId;
        if (value.is(Map.class)) {
            ConfigSection section = value.getAsSection();
            soundId = section.getAssetPath("id");
            volume = section.getValue("volume", SoundValue::fromConfig, volume);
            pitch = section.getValue("pitch", SoundValue::fromConfig, pitch);
        } else {
            soundId = value.getAsAssetPath();
        }
        return new SoundData(soundId, volume, pitch);
    }

    public interface SoundValue extends Supplier<Float> {
        Map<Float, SoundValue> FIXED = Collections.synchronizedMap(new HashMap<>());
        SoundValue FIXED_1 = new Fixed(1f);
        SoundValue FIXED_0_8 = new Fixed(0.8f);
        SoundValue FIXED_0_75 = new Fixed(0.75f);
        SoundValue FIXED_0_15 = new Fixed(0.15f);
        SoundValue FIXED_0_5 = new Fixed(0.5f);
        SoundValue RANGED_0_9_1 = new Ranged(0.9f, 1f);

        static SoundValue fromConfig(ConfigValue config) {
            if (config.is(Number.class)) {
                return fixed(config.getAsFloat());
            } else {
                String stringFormat = config.getAsString();
                if (stringFormat.contains("~")) {
                    ConfigValue[] split = config.splitValuesRestrict("~", 2);
                    return ranged(split[0].getAsFloat(), split[1].getAsFloat());
                } else {
                    return fixed(config.getAsFloat());
                }
            }
        }

        static SoundValue fixed(float value) {
            return FIXED.computeIfAbsent(value, v -> new Fixed(value));
        }

        static SoundValue ranged(float min, float max) {
            if (min > max) {
                return new Ranged(max, min);
            } else if (min == max) {
                return SoundValue.fixed(max);
            }
            return new Ranged(min, max);
        }

        class Fixed implements SoundValue {
            private final float value;

            public Fixed(float value) {
                this.value = value;
            }

            @Override
            public Float get() {
                return this.value;
            }
        }

        class Ranged implements SoundValue {
            private final float min;
            private final float max;

            public Ranged(float min, float max) {
                this.min = min;
                this.max = max;
            }

            @Override
            public Float get() {
                return RandomUtils.generateRandomFloat(this.min, this.max);
            }
        }
    }
}
