package net.momirealms.craftengine.proxy.minecraft.tags;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.tags.PaintingVariantTags")
public interface PaintingVariantTagsProxy {
    PaintingVariantTagsProxy INSTANCE = ASMProxyFactory.create(PaintingVariantTagsProxy.class);
    Object PLACEABLE = INSTANCE.getPlaceable();

    @FieldGetter(name = "PLACEABLE", isStatic = true)
    Object getPlaceable();
}
