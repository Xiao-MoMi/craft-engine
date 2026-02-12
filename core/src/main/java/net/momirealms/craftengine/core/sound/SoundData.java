package net.momirealms.craftengine.core.sound;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record SoundData(Key id, SoundValue volume, SoundValue pitch) {
    public static final SoundData EMPTY = new SoundData(Key.of("minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1);

    public static SoundData create(Object obj, SoundValue volume, SoundValue pitch) {
        if (obj instanceof String key) {
            return new SoundData(Key.of(key), volume, pitch);
        } else if (obj instanceof Map<?,?> map) {
            Map<String, Object> data = MiscUtils.castToMap(map, false);
            Key id = Key.of((String) data.get("id"));
            SoundValue volumeValue = Optional.ofNullable(SoundValue.of(map.get("volume"))).orElse(volume);
            SoundValue pitchValue = Optional.ofNullable(SoundValue.of(map.get("pitch"))).orElse(volume);
            return new SoundData(id, volumeValue, pitchValue);
        } else {
            throw new IllegalArgumentException("Illegal object type for sound data: " + obj.getClass());
        }
    }

    public static SoundData of(Key id, SoundValue volume, SoundValue pitch) {
        return new SoundData(id, volume, pitch);
    }

    public interface SoundValue extends Supplier<Float> {
        Map<Float, SoundValue> FIXED = new HashMap<>();
        SoundValue FIXED_1 = new Fixed(1f);
        SoundValue FIXED_0_8 = new Fixed(0.8f);
        SoundValue FIXED_0_75 = new Fixed(0.75f);
        SoundValue FIXED_0_15 = new Fixed(0.15f);
        SoundValue FIXED_0_5 = new Fixed(0.5f);
        SoundValue FIXED_0_3 = new Fixed(0.3f);

        static SoundValue of(Object obj) {
            if (obj instanceof Number number) {
                return SoundValue.fixed(number.floatValue());
            } else {
                String volumeString = obj.toString();
                if (volumeString.contains("~")) {
                    String[] split = volumeString.split("~");
                    return SoundValue.ranged(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
                }
            }
            return null;
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
