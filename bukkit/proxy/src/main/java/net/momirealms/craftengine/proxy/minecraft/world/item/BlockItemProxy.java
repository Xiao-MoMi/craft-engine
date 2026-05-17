package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.world.item.context.BlockPlaceContextProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.BlockItem")
public interface BlockItemProxy {
    BlockItemProxy INSTANCE = ASMProxyFactory.create(BlockItemProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.BlockItem");

    @MethodInvoker(name = "getBlock")
    Object getBlock(Object target);

    @MethodInvoker(name = "place")
    Object place(Object target, @Type(clazz = BlockPlaceContextProxy.class) Object context);
}
