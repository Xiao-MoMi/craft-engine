package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundAddEntityPacket")
public interface ClientboundAddEntityPacketProxy {
    ClientboundAddEntityPacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundAddEntityPacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundAddEntityPacketProxy.class);

    @FieldGetter(name = "type")
    Object type(Object instance);

    @FieldSetter(name = "type", strategy = Strategy.MH)
    void type(Object instance, Object type);
}
