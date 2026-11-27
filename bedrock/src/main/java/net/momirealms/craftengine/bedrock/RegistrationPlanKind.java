package net.momirealms.craftengine.bedrock;

public enum RegistrationPlanKind {
    ITEM("craftengine:item_registration_plan", "craftengine/items.json", 1),
    BLOCK("craftengine:block_registration_plan", "craftengine/blocks.json", 1);

    private final String id;
    private final String file;
    private final int formatVersion;

    RegistrationPlanKind(String id, String file, int formatVersion) {
        this.id = id;
        this.file = file;
        this.formatVersion = formatVersion;
    }

    public String id() {
        return this.id;
    }

    public String file() {
        return this.file;
    }

    public int formatVersion() {
        return this.formatVersion;
    }
}
