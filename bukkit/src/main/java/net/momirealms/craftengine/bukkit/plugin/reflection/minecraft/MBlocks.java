package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class MBlocks {
    private MBlocks() {}

    public static final Object AIR;
    public static final Object AIR$defaultState;
    public static final Object STONE;
    public static final Object STONE$defaultState;
    public static final Object FIRE;
    public static final Object SOUL_FIRE;
    public static final Object ICE;
    public static final Object SHORT_GRASS;
    public static final Object SHORT_GRASS$defaultState;
    public static final Object SHULKER_BOX;
    public static final Object COMPOSTER;

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, rl);
    }

    static {
        AIR = getById("air");
        AIR$defaultState = FastNMS.INSTANCE.method$Block$defaultState(AIR);
        FIRE = getById("fire");
        SOUL_FIRE = getById("soul_fire");
        STONE = getById("stone");
        STONE$defaultState = FastNMS.INSTANCE.method$Block$defaultState(STONE);
        ICE = getById("ice");
        SHORT_GRASS = getById(VersionHelper.isOrAbove1_20_3() ? "short_grass" : "grass");
        SHORT_GRASS$defaultState = FastNMS.INSTANCE.method$Block$defaultState(SHORT_GRASS);
        SHULKER_BOX = getById("shulker_box");
        COMPOSTER = getById("composter");
    }
}
