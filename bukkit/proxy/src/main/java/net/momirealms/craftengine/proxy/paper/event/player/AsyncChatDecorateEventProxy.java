package net.momirealms.craftengine.proxy.paper.event.player;

import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@SuppressWarnings("UnstableApiUsage")
@ReflectionProxy(clazz = AsyncChatDecorateEvent.class)
public interface AsyncChatDecorateEventProxy {
    AsyncChatDecorateEventProxy INSTANCE = ASMProxyFactory.create(AsyncChatDecorateEventProxy.class);

    @FieldGetter(name = "originalMessage")
    Object getOriginalMessage(Object target);

    @FieldSetter(name = "originalMessage")
    void setOriginalMessage(Object target, Object message);

    @FieldGetter(name = "result")
    Object getResult(Object target);

    @FieldSetter(name = "result")
    void setResult(Object target, Object result);
}
