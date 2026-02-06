package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.BlockAndTintGetter")
public interface BlockAndTintGetterProxy extends BlockGetterProxy {
    BlockAndTintGetterProxy INSTANCE = ASMProxyFactory.create(BlockAndTintGetterProxy.class);

    @MethodInvoker(name = "getLightEngine")
    Object getLightEngine(Object target);
}
