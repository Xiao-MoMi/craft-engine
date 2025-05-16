package net.momirealms.craftengine.core.entity;

public enum Billboard {
    FIXED(0),
    VERTICAL(1),
    HORIZONTAL(2),
    CENTER(3);

    private final byte id;

    Billboard(int index) {
        this.id = (byte) index;
    }

    public byte id() {
        return id;
    }
}
