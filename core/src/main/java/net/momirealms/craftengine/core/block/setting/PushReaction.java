package net.momirealms.craftengine.core.block.setting;

public enum PushReaction {
    // Keep legacy constants (and their ordinals) for existing configurations and API users.
    @Deprecated
    NORMAL(0, 0),
    @Deprecated
    DESTROY(1, 2),
    @Deprecated
    BLOCK(2, 3),
    @Deprecated
    IGNORE(3, 4),
    @Deprecated
    PUSH_ONLY(4, 1),

    PUSH_PULL(0, 0),
    PUSH(4, 1),
    POPPED(1, 2),
    IMMOVEABLE(2, 3),
    IGNORE_ENTITY(3, 4);

    public static final PushReaction[] VALUES = values();

    private final int legacyIndex;
    private final int modernIndex;

    PushReaction(int legacyIndex, int modernIndex) {
        this.legacyIndex = legacyIndex;
        this.modernIndex = modernIndex;
    }

    /** Returns the native values() index, independent of obfuscated enum names. */
    public int minecraftIndex(boolean isOrAbove26_3) {
        return isOrAbove26_3 ? this.modernIndex : this.legacyIndex;
    }
}
