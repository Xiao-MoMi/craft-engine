package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class MAttributeHolders {
    private MAttributeHolders() {}

    public static final Object BLOCK_BREAK_SPEED;
    public static final Object BLOCK_INTERACTION_RANGE;
    public static final Object SCALE;

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getHolderByResourceLocation(MBuiltInRegistries.ATTRIBUTE, rl).get();
    }

    static {
        if (VersionHelper.isOrAbove1_20_5()) {
            BLOCK_BREAK_SPEED = getById(VersionHelper.isOrAbove1_21_2() ? "block_break_speed" : "player.block_break_speed");
            BLOCK_INTERACTION_RANGE = getById(VersionHelper.isOrAbove1_21_2() ? "block_interaction_range" : "player.block_interaction_range");
            SCALE = getById(VersionHelper.isOrAbove1_21_2() ? "scale" : "generic.scale");
        } else {
            BLOCK_BREAK_SPEED = null;
            BLOCK_INTERACTION_RANGE = null;
            SCALE = null;
        }
    }
}
