package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.server.level.ServerPlayerGameMode")
public interface ServerPlayerGameModeProxy {
    ServerPlayerGameModeProxy INSTANCE = ASMProxyFactory.create(ServerPlayerGameModeProxy.class);

    @FieldGetter(name = "gameTicks")
    int getGameTicks(Object target);

    @FieldGetter(name = "isDestroyingBlock")
    boolean isDestroyingBlock(Object target);

    @FieldSetter(name = "isDestroyingBlock")
    void setIsDestroyingBlock(Object target, boolean isDestroyingBlock);

    @MethodInvoker(name = "destroyBlock")
    boolean destroyBlock(Object target, @Type(clazz = BlockPosProxy.class) Object pos);
}
