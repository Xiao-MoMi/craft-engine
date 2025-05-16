package net.momirealms.craftengine.core.entity;

public enum ItemDisplayContext {
    NONE(0),
    THIRD_PERSON_LEFT_HAND(1),
    THIRD_PERSON_RIGHT_HAND(2),
    FIRST_PERSON_LEFT_HAND(3),
    FIRST_PERSON_RIGHT_HAND(4),
    HEAD(5),
    GUI(6),
    GROUND(7),
    FIXED(8);

    private final byte id;

    ItemDisplayContext(int index) {
        this.id = (byte) index;
    }

    public byte id() {
        return id;
    }
}
