package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundSwingPacket")
public interface ServerboundSwingPacketProxy extends PacketProxy {
    ServerboundSwingPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundSwingPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundSwingPacket");

    @FieldGetter(name = "hand")
    Object getHand(Object target);

    @FieldSetter(name = "hand")
    void setHand(Object target, Object hand);
}
