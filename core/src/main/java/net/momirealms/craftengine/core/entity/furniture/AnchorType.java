package net.momirealms.craftengine.core.entity.furniture;

public enum AnchorType {
    GROUND(0),
    WALL(1),
    CEILING(2);

    private final int id;

    AnchorType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static AnchorType byId(int id) {
        return values()[id];
    }
}
