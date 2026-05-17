package net.momirealms.craftengine.proxy.minecraft.world.level.block.state;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockState")
public interface BlockStateProxy extends BlockBehaviourProxy.BlockStateBaseProxy {
    BlockStateProxy INSTANCE = ASMProxyFactory.create(BlockStateProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.state.BlockState");
}
