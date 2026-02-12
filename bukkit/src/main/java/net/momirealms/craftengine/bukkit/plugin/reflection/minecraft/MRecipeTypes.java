package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MRecipeTypes {
    private MRecipeTypes() {}

    public static final Object CRAFTING = getById("crafting");
    public static final Object SMELTING = getById("smelting");
    public static final Object BLASTING = getById("blasting");
    public static final Object SMOKING = getById("smoking");
    public static final Object CAMPFIRE_COOKING = getById("campfire_cooking");
    public static final Object STONECUTTING = getById("stonecutting");
    public static final Object SMITHING = getById("smithing");

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(MBuiltInRegistries.RECIPE_TYPE, id);
    }
}
