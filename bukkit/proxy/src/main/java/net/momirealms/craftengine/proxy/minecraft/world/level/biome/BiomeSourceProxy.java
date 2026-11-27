package net.momirealms.craftengine.proxy.minecraft.world.level.biome;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Set;

@ReflectionProxy(name = "net.minecraft.world.level.biome.BiomeSource")
public interface BiomeSourceProxy {
    BiomeSourceProxy INSTANCE = ASMProxyFactory.create(BiomeSourceProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.biome.BiomeSource");

    @MethodInvoker(name = "possibleBiomes")
    Set<Object> possibleBiomes(Object target);
}
