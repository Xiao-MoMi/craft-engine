package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.LevelWriter")
public interface LevelWriterProxy {
    LevelWriterProxy INSTANCE = ASMProxyFactory.create(LevelWriterProxy.class);

    @MethodInvoker(name = "destroyBlock")
    boolean destroyBlock(Object target, @Type(clazz = BlockPosProxy.class) Object pos, boolean drop);
}
