package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.util.VersionHelper;

public final class UpdateFlags {
    private UpdateFlags() {}

    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE = 128;
    public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 256;
    public static final int UPDATE_SKIP_ON_PLACE = 512;
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    public static final int UPDATE_NONE = VersionHelper.isOrAbove1_21_5() ? 260 : 4;
    public static final int UPDATE_SKIP_ALL_SIDEEFFECTS = 816; // 1.21.5
    public static final int UPDATE_NO_PHYS = VersionHelper.isOrAbove1_21_5() ? 530 : 1042;
}
