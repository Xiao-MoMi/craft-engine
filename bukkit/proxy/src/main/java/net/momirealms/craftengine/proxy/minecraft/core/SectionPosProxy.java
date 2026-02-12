package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.SectionPos")
public interface SectionPosProxy extends Vec3iProxy {
    SectionPosProxy INSTANCE = ASMProxyFactory.create(SectionPosProxy.class);

    @ConstructorInvoker
    Object newInstance(int x, int y, int z);
}
