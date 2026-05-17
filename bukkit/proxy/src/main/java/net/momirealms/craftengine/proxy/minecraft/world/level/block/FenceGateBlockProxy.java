package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.FenceGateBlock")
public interface FenceGateBlockProxy extends BlockProxy {
    FenceGateBlockProxy INSTANCE = ASMProxyFactory.create(FenceGateBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.FenceGateBlock");

    @MethodInvoker(name = "connectsToDirection", isStatic = true)
    boolean connectsToDirection(@Type(clazz = BlockStateProxy.class) Object state, @Type(clazz = DirectionProxy.class) Object side);
}
