package net.momirealms.craftengine.core.entity.furniture;

import java.util.function.Function;

public enum RotationRule {
    ANY(Function.identity()),
    FOUR(yaw -> (float) (Math.round(yaw / 90) * 90)),
    EIGHT(yaw -> (float) (Math.round(yaw / 45) * 45)),
    SIXTEEN(yaw -> (float) (Math.round(yaw / 22.5) * 22.5)),
    NORTH(__ -> 180f),
    EAST(__ -> -90f),
    WEST(__ -> 90f),
    SOUTH(__ -> 0f);

    private final Function<Float, Float> function;

    RotationRule(Function<Float, Float> function) {
        this.function = function;
    }

    public float apply(float yaw) {
        return function.apply(yaw);
    }
}
