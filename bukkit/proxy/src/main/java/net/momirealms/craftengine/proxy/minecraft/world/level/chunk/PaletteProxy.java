package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.Palette")
public interface PaletteProxy {
    PaletteProxy INSTANCE = ASMProxyFactory.create(PaletteProxy.class);


}
