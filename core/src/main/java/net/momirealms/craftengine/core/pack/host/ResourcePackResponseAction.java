package net.momirealms.craftengine.core.pack.host;

public enum ResourcePackResponseAction {
    SUCCESSFULLY_LOADED(false),
    DECLINED(false),
    FAILED_DOWNLOAD(false),
    ACCEPTED(true),
    DOWNLOADED(true),
    INVALID_URL(false),
    FAILED_RELOAD(false),
    DISCARDED(false),
    UNKNOWN(false); // 未知的动作，可能是新版本添加的

    private final boolean intermediate;

    ResourcePackResponseAction(final boolean intermediate) {
        this.intermediate = intermediate;
    }

    public boolean intermediate() {
        return this.intermediate;
    }

    public static ResourcePackResponseAction byOrdinal(int ordinal) {
        ResourcePackResponseAction[] values = values();
        if (ordinal < 0 || ordinal >= values.length) { // 为了以防万一还是加一个判断吧
            return UNKNOWN;
        }
        return values[ordinal];
    }
}
