package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.SignalGetter")
public interface SignalGetterProxy extends BlockGetterProxy {
    SignalGetterProxy INSTANCE = ASMProxyFactory.create(SignalGetterProxy.class);

    @MethodInvoker(name = "hasNeighborSignal")
    boolean hasNeighborSignal(Object target, @Type(clazz = BlockPosProxy.class) Object pos);
}
