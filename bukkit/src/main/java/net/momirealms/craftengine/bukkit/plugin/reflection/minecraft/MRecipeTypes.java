package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;

public final class MRecipeTypes {
    private MRecipeTypes() {}

    public static final Object CRAFTING;
    public static final Object SMELTING;
    public static final Object BLASTING;
    public static final Object SMOKING;
    public static final Object CAMPFIRE_COOKING;
    public static final Object STONECUTTING;
    public static final Object SMITHING;

    private static Object getById(String id) {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.RECIPE_TYPE, rl);
    }

    static {
        CRAFTING = getById("crafting");
        SMELTING = getById("smelting");
        BLASTING = getById("blasting");
        SMOKING = getById("smoking");
        CAMPFIRE_COOKING = getById("campfire_cooking");
        STONECUTTING = getById("stonecutting");
        SMITHING = getById("smithing");
    }
}
