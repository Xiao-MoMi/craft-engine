package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.WorldlyContainerHolder")
public interface WorldlyContainerHolderProxy {
    WorldlyContainerHolderProxy INSTANCE = ASMProxyFactory.create(WorldlyContainerHolderProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.WorldlyContainerHolder");

    @MethodInvoker(name = "getContainer")
    Object getContainer(Object target, @Type(clazz = BlockStateProxy.class) Object state, @Type(clazz = LevelAccessorProxy.class) Object level, @Type(clazz = BlockPosProxy.class) Object pos);
}
