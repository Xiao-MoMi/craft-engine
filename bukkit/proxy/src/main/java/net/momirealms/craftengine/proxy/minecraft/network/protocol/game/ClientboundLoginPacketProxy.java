package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundLoginPacket")
public interface ClientboundLoginPacketProxy {
    ClientboundLoginPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLoginPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundLoginPacket");

    @FieldGetter(name = "dimension", activeIf = "max_version=1.20.1")
    Object getDimension(Object target);

    @FieldGetter(name = "commonPlayerSpawnInfo", activeIf = "min_version=1.20.2")
    Object getCommonPlayerSpawnInfo(Object target);

    @FieldSetter(name = "enforcesSecureChat", activeIf = "min_version=1.20.5")
    void setEnforcesSecureChat(Object target, boolean value);
}
