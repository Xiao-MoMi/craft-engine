package net.momirealms.craftengine.proxy.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.HashMapPalette")
public interface HashMapPaletteProxy {
    HashMapPaletteProxy INSTANCE = ASMProxyFactory.create(HashMapPaletteProxy.class);
}
