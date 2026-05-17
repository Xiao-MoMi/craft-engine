package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.DyeColor")
public interface DyeColorProxy {
    DyeColorProxy INSTANCE = ASMProxyFactory.create(DyeColorProxy.class);

    @FieldGetter(name = "fireworkColor")
    int getFireworkColor(Object target);

    @FieldGetter(name = "textureDiffuseColor", activeIf = "min_version=1.21")
    int getTextureDiffuseColor(Object target);

    @FieldGetter(name = "textureDiffuseColors", activeIf = "max_version=1.20.6")
    float[] getTextureDiffuseColors(Object target);
}
