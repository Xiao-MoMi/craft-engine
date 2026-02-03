package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket")
public interface ClientboundSetActionBarTextPacketProxy {
    ClientboundSetActionBarTextPacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundSetActionBarTextPacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundSetActionBarTextPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ComponentProxy.class) Object message);

    @FieldGetter(name = "text")
    Object text(Object instance);

    @FieldSetter(name = "text", strategy = Strategy.MH)
    void text(Object instance, Object text);
}
