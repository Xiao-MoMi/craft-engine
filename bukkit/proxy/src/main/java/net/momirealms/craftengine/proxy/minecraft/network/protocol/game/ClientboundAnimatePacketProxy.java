package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.UnsafeConstructor;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundAnimatePacket")
public interface ClientboundAnimatePacketProxy extends PacketProxy {
    ClientboundAnimatePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundAnimatePacketProxy.class);
    UnsafeConstructor UNSAFE_CONSTRUCTOR = new UnsafeConstructor(SparrowClass.find("net.minecraft.network.protocol.game.ClientboundAnimatePacket"));

    @FieldGetter(name = "id")
    int getId(Object target);

    @FieldSetter(name = "id")
    void setId(Object target, int id);

    @FieldGetter(name = "action")
    int getAction(Object target);

    @FieldSetter(name = "action")
    void setAction(Object target, int action);
}
