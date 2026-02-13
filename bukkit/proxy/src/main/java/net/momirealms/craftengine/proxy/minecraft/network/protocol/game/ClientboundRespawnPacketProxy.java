package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundRespawnPacket")
public interface ClientboundRespawnPacketProxy {
    ClientboundRespawnPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundRespawnPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundRespawnPacket");

    @FieldGetter(name = "dimension", activeIf = "max_version=1.20.1")
    Object getDimension(Object target);

    @FieldGetter(name = "commonPlayerSpawnInfo", activeIf = "min_version=1.20.2")
    Object getCommonPlayerSpawnInfo(Object target);
}
