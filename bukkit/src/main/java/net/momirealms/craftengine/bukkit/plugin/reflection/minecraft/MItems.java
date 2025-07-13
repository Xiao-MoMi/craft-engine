package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;

public final class MItems {
    private MItems() {}

    public static final Object AIR;
    public static final Object WATER_BUCKET;
    public static final Object BARRIER;

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, rl);
    }

    static {
        AIR = getById("air");
        WATER_BUCKET = getById("water_bucket");
        BARRIER = getById("barrier");
    }
}
