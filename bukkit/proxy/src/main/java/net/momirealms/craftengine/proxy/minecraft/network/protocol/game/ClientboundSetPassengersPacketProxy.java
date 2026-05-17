package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.UnsafeConstructor;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetPassengersPacket")
public interface ClientboundSetPassengersPacketProxy extends PacketProxy {
    ClientboundSetPassengersPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetPassengersPacketProxy.class);
    UnsafeConstructor UNSAFE_CONSTRUCTOR = new UnsafeConstructor(SparrowClass.find("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket"));

    @FieldGetter(name = "vehicle")
    int getVehicle(Object target);

    @FieldGetter(name = "passengers")
    int[] getPassengers(Object target);

    @FieldSetter(name = "vehicle")
    void setVehicle(Object target, int vehicle);

    @FieldSetter(name = "passengers")
    void setPassengers(Object target, int[] passengers);
}
