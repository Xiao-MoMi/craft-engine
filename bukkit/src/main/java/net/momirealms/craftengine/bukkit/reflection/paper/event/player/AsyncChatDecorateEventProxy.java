package net.momirealms.craftengine.bukkit.reflection.paper.event.player;

import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@SuppressWarnings("UnstableApiUsage")
@ReflectionProxy(clazz = AsyncChatDecorateEvent.class)
public interface AsyncChatDecorateEventProxy {
    AsyncChatDecorateEventProxy INSTANCE = ReflectionHelper.getProxy(AsyncChatDecorateEventProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(AsyncChatDecorateEventProxy.class);

    @FieldGetter(name = "originalMessage")
    Object originalMessage(AsyncChatDecorateEvent instance);

    @FieldSetter(name = "originalMessage", strategy = Strategy.MH)
    void originalMessage(AsyncChatDecorateEvent instance, Object originalMessage);

    @FieldGetter(name = "result")
    Object result(AsyncChatDecorateEvent instance);

    @FieldSetter(name = "result")
    void result(AsyncChatDecorateEvent instance, Object result);
}
