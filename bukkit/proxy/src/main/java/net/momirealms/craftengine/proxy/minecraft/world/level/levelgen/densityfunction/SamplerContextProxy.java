package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.densityfunction;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.densityfunction.SamplerContext", activeIf = "min_version=26.3")
public interface SamplerContextProxy {
    SamplerContextProxy INSTANCE = ASMProxyFactory.create(SamplerContextProxy.class);
    Object EMPTY_UNCACHED = INSTANCE != null ? INSTANCE.getEmpty() : null;
    @FieldGetter(name = "EMPTY_UNCACHED", isStatic = true)
    Object getEmpty();
}
