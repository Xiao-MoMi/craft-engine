package net.momirealms.craftengine.bedrock;

public enum BedrockHeldItemType {
    NATIVE(0),
    SPRITE(1),
    HANDHELD(2),
    FISHING_ROD(3),
    BLOCK(4),
    BOW(5),
    SHIELD(6),
    TRIDENT(7),
    SPEAR(8);

    public static final String PROPERTY = "craftengine:held_item_type";
    private final int id;

    BedrockHeldItemType(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }
}
