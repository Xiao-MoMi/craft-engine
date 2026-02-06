package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.StairsShape")
public interface StairsShapeProxy {
    StairsShapeProxy INSTANCE = ASMProxyFactory.create(StairsShapeProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object STRAIGHT = VALUES[0];
    Object INNER_LEFT = VALUES[1];
    Object INNER_RIGHT = VALUES[2];
    Object OUTER_LEFT = VALUES[3];
    Object OUTER_RIGHT = VALUES[4];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
