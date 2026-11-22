package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.RotatedPillarBlock")
public interface RotatedPillarBlockProxy {
    RotatedPillarBlockProxy INSTANCE = ASMProxyFactory.create(RotatedPillarBlockProxy.class);
    Object AXIS = INSTANCE.axis();

    @FieldGetter(name = "AXIS", isStatic = true)
    Object axis();
}
