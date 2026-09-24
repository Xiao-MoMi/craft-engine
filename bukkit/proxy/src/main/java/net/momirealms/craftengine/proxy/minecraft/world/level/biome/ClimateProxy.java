package net.momirealms.craftengine.proxy.minecraft.world.level.biome;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.biome.Climate", activeIf = "min_version=26.3")
public interface ClimateProxy {
    ClimateProxy INSTANCE = ASMProxyFactory.create(ClimateProxy.class);
    @ReflectionProxy(name = "net.minecraft.world.level.biome.Climate$Sampler", activeIf = "min_version=26.3")
    interface SamplerProxy {}
}
