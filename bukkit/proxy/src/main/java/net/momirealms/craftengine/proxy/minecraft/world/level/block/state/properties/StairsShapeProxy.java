package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.StairsShape")
public interface StairsShapeProxy {
    StairsShapeProxy INSTANCE = ASMProxyFactory.create(StairsShapeProxy.class);
    Enum<?>[] VALUES = INSTANCE.values();
    Enum<?> STRAIGHT = VALUES[0];
    Enum<?> INNER_LEFT = VALUES[1];
    Enum<?> INNER_RIGHT = VALUES[2];
    Enum<?> OUTER_LEFT = VALUES[3];
    Enum<?> OUTER_RIGHT = VALUES[4];

    @MethodInvoker(name = "values", isStatic = true)
    Enum<?>[] values();
}
