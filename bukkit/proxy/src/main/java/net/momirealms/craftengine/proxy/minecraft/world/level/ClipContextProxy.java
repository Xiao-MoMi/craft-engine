package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.ClipContext")
public interface ClipContextProxy {
    ClipContextProxy INSTANCE = ASMProxyFactory.create(ClipContextProxy.class);

    @ConstructorInvoker
    Object newInstance(
            @Type(clazz = Vec3Proxy.class) Object from,
            @Type(clazz = Vec3Proxy.class) Object to,
            @Type(clazz = BlockProxy.class) Object blockMode,
            @Type(clazz = FluidProxy.class) Object fluidMode,
            @Type(clazz = EntityProxy.class) Object entity
    );

    @ReflectionProxy(name = "net.minecraft.world.level.ClipContext$Block")
    interface BlockProxy {
        BlockProxy INSTANCE = ASMProxyFactory.create(BlockProxy.class);
        Enum<?> COLLIDER = INSTANCE.values()[0];
        Enum<?> OUTLINE = INSTANCE.values()[1];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }

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
