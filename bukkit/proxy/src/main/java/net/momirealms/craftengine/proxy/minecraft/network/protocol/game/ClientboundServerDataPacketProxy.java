package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundServerDataPacket")
public interface ClientboundServerDataPacketProxy {
    ClientboundServerDataPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundServerDataPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundServerDataPacket");

    @FieldSetter(name = "enforcesSecureChat", activeIf = "max_version=1.20.4")
    void setEnforcesSecureChat(Object target, boolean value);
}
