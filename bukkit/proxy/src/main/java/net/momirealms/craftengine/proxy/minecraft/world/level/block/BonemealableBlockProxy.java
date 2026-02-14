package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.BonemealableBlock")
public interface BonemealableBlockProxy {
    BonemealableBlockProxy INSTANCE = ASMProxyFactory.create(BonemealableBlockProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.BonemealableBlock");

    @MethodInvoker(name = "isValidBonemealTarget", activeIf = "min_version=1.20.2")
    boolean isValidBonemealTarget(Object target, @Type(clazz = LevelReaderProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object state);

    @MethodInvoker(name = "isValidBonemealTarget", activeIf = "max_version=1.20.1")
    boolean isValidBonemealTarget(Object target, @Type(clazz = LevelReaderProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object state, boolean isClient);

    @MethodInvoker(name = "performBonemeal")
    void performBonemeal(Object target, @Type(clazz = ServerLevelProxy.class) Object world, @Type(clazz = RandomSourceProxy.class) Object random, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object state);
}
