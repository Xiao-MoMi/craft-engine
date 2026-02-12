package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.BlockItem")
public interface BlockItemProxy {
    BlockItemProxy INSTANCE = ASMProxyFactory.create(BlockItemProxy.class);

    @MethodInvoker(name = "getBlock")
    Object getBlock(Object target);
}
