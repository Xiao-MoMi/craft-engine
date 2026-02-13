package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket", activeIf = "min_version=1.21.4")
public interface ServerboundPickItemFromBlockPacketProxy {
    ServerboundPickItemFromBlockPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundPickItemFromBlockPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket");

    @FieldGetter(name = "pos")
    Object getPos(Object target);
}
