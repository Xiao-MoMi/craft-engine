package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public final class MItems {
    private MItems() {}

    public static final Object AIR;
    public static final Object WATER_BUCKET;
    public static final Object BARRIER;

    @Nullable
    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, rl);
    }

    @Nullable
    public static Object getById(Key id) {
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, KeyUtils.toResourceLocation(id));
    }

    static {
        AIR = getById("air");
        WATER_BUCKET = getById("water_bucket");
        BARRIER = getById("barrier");
    }
}
