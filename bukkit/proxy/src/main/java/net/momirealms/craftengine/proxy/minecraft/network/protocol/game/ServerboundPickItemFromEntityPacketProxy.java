package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket", activeIf = "min_version=1.21.4")
public interface ServerboundPickItemFromEntityPacketProxy {
    ServerboundPickItemFromEntityPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundPickItemFromEntityPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket");

    @FieldGetter(name = "id")
    int getId(Object target);
}
