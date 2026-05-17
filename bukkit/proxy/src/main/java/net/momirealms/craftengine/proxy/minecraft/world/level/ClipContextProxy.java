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
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> NONE = VALUES[0];
        Enum<?> SOURCE_ONLY = VALUES[1];
        Enum<?> ANY = VALUES[2];
        Enum<?> WATER = VALUES[3];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }
}
