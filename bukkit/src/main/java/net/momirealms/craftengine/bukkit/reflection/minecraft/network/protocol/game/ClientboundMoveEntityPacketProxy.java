package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket")
public interface ClientboundMoveEntityPacketProxy {
    ClientboundMoveEntityPacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundMoveEntityPacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundMoveEntityPacketProxy.class);

    @FieldGetter(name = "entityId")
    int entityId(Object instance);

    @FieldSetter(name = "entityId", strategy = Strategy.MH)
    void entityId(Object instance, int entityId);
}
