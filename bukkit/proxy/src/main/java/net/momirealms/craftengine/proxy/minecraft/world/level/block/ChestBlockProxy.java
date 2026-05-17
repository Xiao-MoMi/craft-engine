package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.ChestBlock")
public interface ChestBlockProxy {
    ChestBlockProxy INSTANCE = ASMProxyFactory.create(ChestBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.ChestBlock");

    @MethodInvoker(name = "getContainer", isStatic = true)
    Object getContainer(@Type(clazz = ChestBlockProxy.class) Object block, @Type(clazz = BlockStateProxy.class) Object state, @Type(clazz = LevelProxy.class) Object level, @Type(clazz = BlockPosProxy.class) Object pos, boolean ignoreBlocked);
}
