package net.momirealms.craftengine.proxy.minecraft.world.level.gameevent;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.gameevent.BlockPositionSource")
public interface BlockPositionSourceProxy {
    BlockPositionSourceProxy INSTANCE = ASMProxyFactory.create(BlockPositionSourceProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = BlockPosProxy.class) Object pos);
}
