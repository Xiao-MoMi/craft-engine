package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.Blocks")
public interface BlocksProxy {
    BlocksProxy INSTANCE = ASMProxyFactory.create(BlocksProxy.class);
    Object COBWEB = INSTANCE.getCobweb();

    @FieldGetter(name = "COBWEB", isStatic = true)
    Object getCobweb();
}
