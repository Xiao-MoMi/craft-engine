package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.Instrument", activeIf = "min_version=1.21.2 && max_version=1.21.11")
public interface InstrumentProxy {
    InstrumentProxy INSTANCE = ASMProxyFactory.create(InstrumentProxy.class);

    @MethodInvoker(name = "useDuration")
    float useDuration(Object target);
}
