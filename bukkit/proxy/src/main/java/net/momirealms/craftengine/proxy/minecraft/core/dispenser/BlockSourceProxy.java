package net.momirealms.craftengine.proxy.minecraft.core.dispenser;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.dispenser.BlockSource")
public interface BlockSourceProxy {
    BlockSourceProxy INSTANCE = ASMProxyFactory.create(BlockSourceProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.dispenser.BlockSource");

    @MethodInvoker(name = "level", activeIf = "min_version=1.20.5")
    Object level(Object target);

    @MethodInvoker(name = "pos", activeIf = "min_version=1.20.5")
    Object pos(Object target);

    @MethodInvoker(name = "state", activeIf = "min_version=1.20.5")
    Object state(Object target);

    @MethodInvoker(name = "blockEntity", activeIf = "min_version=1.20.5")
    Object blockEntity(Object target);
}
