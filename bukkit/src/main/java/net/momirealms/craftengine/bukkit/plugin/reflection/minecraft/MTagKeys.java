package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;

import java.util.Objects;

public final class MTagKeys {
    private MTagKeys() {}

    public static final Object Fluid$WATER = create(MRegistries.FLUID, "water");
    public static final Object Item$WOOL = create(MRegistries.ITEM, "wool");
    public static final Object Block$WALLS = create(MRegistries.BLOCK, "walls");
    public static final Object Block$SHULKER_BOXES = create(MRegistries.BLOCK, "shulker_boxes");
    public static final Object Block$FENCES = create(MRegistries.BLOCK, "fences");
    public static final Object Block$WOODEN_FENCES = create(MRegistries.BLOCK, "wooden_fences");
    public static final Object Block$DIRT = create(MRegistries.BLOCK, "dirt");
    public static final Object Block$SNOW = create(MRegistries.BLOCK, "snow");

    private static Object create(Object registry, String location) {
        Object identifier = IdentifierProxy.INSTANCE.newInstance("minecraft", location);
        Object tagKey = TagKeyProxy.INSTANCE.create(registry, identifier);
        return Objects.requireNonNull(tagKey);
    }
}
