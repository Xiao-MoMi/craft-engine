package net.momirealms.craftengine.core.advancement;

public enum AdvancementType {
    TASK("task"),
    CHALLENGE("challenge"),
    GOAL("goal"),;

    private final String id;

    AdvancementType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static final AdvancementType[] VALUES = values();

    public static AdvancementType byId(int id) {
        return VALUES[id];
    }
}
