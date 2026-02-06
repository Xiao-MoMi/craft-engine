package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.HashMapPalette")
public interface HashMapPaletteProxy extends PaletteProxy {
    HashMapPaletteProxy INSTANCE = ASMProxyFactory.create(HashMapPaletteProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.chunk.HashMapPalette");
    
    @FieldGetter(name = "values")
    Object getValues(Object target);
}
