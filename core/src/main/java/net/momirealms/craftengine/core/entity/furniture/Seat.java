package net.momirealms.craftengine.core.entity.furniture;

import org.joml.Vector3f;

import java.util.Objects;

public record Seat(Vector3f offset, float yaw, boolean limitPlayerRotation) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return Float.compare(yaw, seat.yaw) == 0 && Objects.equals(offset, seat.offset) && limitPlayerRotation == seat.limitPlayerRotation;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(offset);
        result = 31 * result + Float.hashCode(yaw);
        result = 31 * result + Boolean.hashCode(limitPlayerRotation);
        return result;
    }
}
