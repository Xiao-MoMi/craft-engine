package net.momirealms.craftengine.proxy.minecraft.world.level.biome;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.biome.Biome")
public interface BiomeProxy {
    BiomeProxy INSTANCE = ASMProxyFactory.create(BiomeProxy.class);

    @ReflectionProxy(name = "net.minecraft.world.level.biome.Biome$Precipitation")
    interface PrecipitationProxy {
        PrecipitationProxy INSTANCE = ASMProxyFactory.create(PrecipitationProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.biome.Biome$Precipitation");
    }
}
