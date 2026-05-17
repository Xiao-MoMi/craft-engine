package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LevelLightEngineProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.level.ThreadedLevelLightEngine")
public interface ThreadedLevelLightEngineProxy extends LevelLightEngineProxy {
    ThreadedLevelLightEngineProxy INSTANCE = ASMProxyFactory.create(ThreadedLevelLightEngineProxy.class);

    @MethodInvoker(name = "checkBlock")
    void checkBlock(Object target, @Type(clazz = BlockPosProxy.class) Object pos);
}
