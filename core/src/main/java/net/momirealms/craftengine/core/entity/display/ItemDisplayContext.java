package net.momirealms.craftengine.core.entity.display;

public enum ItemDisplayContext {
    NONE(0, "none"),
    THIRD_PERSON_LEFT_HAND(1, "thirdperson_lefthand"),
    THIRD_PERSON_RIGHT_HAND(2, "thirdperson_righthand"),
    FIRST_PERSON_LEFT_HAND(3, "firstperson_lefthand"),
    FIRST_PERSON_RIGHT_HAND(4, "firstperson_righthand"),
    HEAD(5, "head"),
    GUI(6, "gui"),
    GROUND(7, "ground"),
    FIXED(8, "fixed"),
    ON_SHELF(9, "on_shelf");

    private final byte id;
    private final String serializedName;

    ItemDisplayContext(int index, String serializedName) {
        this.id = (byte) index;
        this.serializedName = serializedName;
    }

    public byte id() {
        return this.id;
    }

    public String serializedName() {
        return this.serializedName;
    }
}
