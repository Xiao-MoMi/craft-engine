package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.SingleValuePalette")
public interface SingleValuePaletteProxy extends PaletteProxy {
    SingleValuePaletteProxy INSTANCE = ASMProxyFactory.create(SingleValuePaletteProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.chunk.SingleValuePalette");

    @FieldGetter(name = "value")
    Object getValue(Object target);
}
