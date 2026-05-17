package net.momirealms.craftengine.core.block.setting;

public enum PushReaction {
    NORMAL,
    DESTROY,
    BLOCK,
    IGNORE,
    PUSH_ONLY;

    public static final PushReaction[] VALUES = values();
}
