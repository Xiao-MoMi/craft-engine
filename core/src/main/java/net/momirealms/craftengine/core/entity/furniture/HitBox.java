package net.momirealms.craftengine.core.entity.furniture;

import org.joml.Vector3f;

public class HitBox {
    private final Vector3f position;
    private final Vector3f size;
    private final Seat[] seats;
    private final boolean responsive;

    public HitBox(Vector3f position, Vector3f size, Seat[] seats, boolean responsive) {
        this.position = position;
        this.size = size;
        this.seats = seats;
        this.responsive = responsive;
    }

    public boolean responsive() {
        return responsive;
    }

    public Seat[] seats() {
        return seats;
    }

    public Vector3f offset() {
        return position;
    }

    public Vector3f size() {
        return size;
    }
}
