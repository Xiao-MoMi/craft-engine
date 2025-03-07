package net.momirealms.craftengine.core.pack.obfuscation;

public enum ResourceType {
    SOUND("sounds", ".ogg"),
    BLOCKSTATE("blockstates", ".json"),
    FONT("font", ".json"),
    ITEM("items", ".json"),
    MODEL("models", ".json"),
    TEXTURE("textures", ".png");

    private final String typeName;
    private final String suffix;

    ResourceType(String typeName, String suffix) {
        this.typeName = typeName;
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public String typeName() {
        return typeName;
    }

    public static ResourceType of(String typeName) {
        for (ResourceType type : values()) {
            if (type.typeName.equals(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + typeName);
    }
}
