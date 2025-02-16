package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FurnitureSounds {
    public static final Key EMPTY_SOUND = Key.of("minecraft:intentionally_empty");
    public static final FurnitureSounds EMPTY = new FurnitureSounds(EMPTY_SOUND, EMPTY_SOUND, EMPTY_SOUND);

    private final Key breakSound;
    private final Key placeSound;
    private final Key rotateSound;

    public FurnitureSounds(Key breakSound, Key placeSound, Key rotateSound) {
        this.breakSound = breakSound;
        this.placeSound = placeSound;
        this.rotateSound = rotateSound;
    }

    public static FurnitureSounds fromMap(Map<String, Object> map) {
        if (map == null) return EMPTY;
        return new FurnitureSounds(
                Key.of(map.getOrDefault("break", "minecraft:intentionally_empty").toString()),
                Key.of(map.getOrDefault("step", "minecraft:intentionally_empty").toString()),
                Key.of(map.getOrDefault("place", "minecraft:intentionally_empty").toString())
        );
    }

    public Key breakSound() {
        return breakSound;
    }

    public Key placeSound() {
        return placeSound;
    }

    public Key rotateSound() {
        return rotateSound;
    }
}
