package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.block.BonemealableBlock")
public interface BonemealableBlockProxy {
    BonemealableBlockProxy INSTANCE = ASMProxyFactory.create(BonemealableBlockProxy.class);

    @MethodInvoker(name = "isValidBonemealTarget", activeIf = "min_version=1.20.2")
    boolean isValidBonemealTarget(Object target, @Type(clazz = LevelReaderProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object state);

    @MethodInvoker(name = "isValidBonemealTarget", activeIf = "max_version=1.20.1")
    boolean isValidBonemealTarget(Object target, @Type(clazz = LevelReaderProxy.class) Object world, @Type(clazz = BlockPosProxy.class) Object pos, @Type(clazz = BlockStateProxy.class) Object state, boolean isClient);
}
