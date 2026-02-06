package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.SupportType")
public interface SupportTypeProxy {
    SupportTypeProxy INSTANCE = ASMProxyFactory.create(SupportTypeProxy.class);
    Object[] VALUES = INSTANCE.values();
    Object FULL = VALUES[0];
    Object CENTER = VALUES[1];
    Object RIGID = VALUES[2];

    @MethodInvoker(name = "values", isStatic = true)
    Object[] values();
}
