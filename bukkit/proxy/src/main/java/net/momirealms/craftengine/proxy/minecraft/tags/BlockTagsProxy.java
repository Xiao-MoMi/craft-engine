package net.momirealms.craftengine.proxy.minecraft.tags;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.tags.BlockTags")
public interface BlockTagsProxy {
    BlockTagsProxy INSTANCE = ASMProxyFactory.create(BlockTagsProxy.class);
    Object WALLS = INSTANCE.getWalls();
    Object SHULKER_BOXES = INSTANCE.getShulkerBoxes();
    Object FENCES = INSTANCE.getFences();
    Object WOODEN_FENCES = INSTANCE.getWoodenFences();
    Object SNOW = INSTANCE.getSnow();

    @FieldGetter(name = "WALLS", isStatic = true)
    Object getWalls();

    @FieldGetter(name = "SHULKER_BOXES", isStatic = true)
    Object getShulkerBoxes();

    @FieldGetter(name = "FENCES", isStatic = true)
    Object getFences();

    @FieldGetter(name = "WOODEN_FENCES", isStatic = true)
    Object getWoodenFences();

    @FieldGetter(name = "SNOW", isStatic = true)
    Object getSnow();

}
