package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.level.ServerLevel")
public interface ServerLevelProxy extends LevelProxy {
    ServerLevelProxy INSTANCE = ASMProxyFactory.create(ServerLevelProxy.class);

    @MethodInvoker(name = "sendBlockUpdated")
    void sendBlockUpdated(Object target,
                          @Type(clazz = BlockPosProxy.class) Object pos,
                          @Type(clazz = BlockStateProxy.class) Object oldState,
                          @Type(clazz = BlockStateProxy.class) Object newState,
                          int flags);

    @MethodInvoker(name = "getEntityLookup", activeIf = "max_version=1.20.6")
    Object getEntityLookup(Object target);

    @MethodInvoker(name = "getChunkSource")
    Object getChunkSource(Object target);
}
