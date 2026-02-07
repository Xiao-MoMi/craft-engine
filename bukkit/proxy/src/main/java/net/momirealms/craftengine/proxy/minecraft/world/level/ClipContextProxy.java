package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.ClipContext")
public interface ClipContextProxy {
    ClipContextProxy INSTANCE = ASMProxyFactory.create(ClipContextProxy.class);

    @ReflectionProxy(name = "net.minecraft.world.level.ClipContext$Fluid")
    interface FluidProxy {
        FluidProxy INSTANCE = ASMProxyFactory.create(FluidProxy.class);
        Object[] VALUES = INSTANCE.values();
        Object NONE = VALUES[0];
        Object SOURCE_ONLY = VALUES[1];
        Object ANY = VALUES[2];
        Object WATER = VALUES[3];

        @MethodInvoker(name = "values", isStatic = true)
        Object[] values();
    }
}
