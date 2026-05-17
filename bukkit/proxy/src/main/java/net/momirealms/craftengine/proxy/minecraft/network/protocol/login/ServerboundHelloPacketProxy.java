package net.momirealms.craftengine.proxy.minecraft.network.protocol.login;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Optional;
import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.login.ServerboundHelloPacket")
public interface ServerboundHelloPacketProxy {
    ServerboundHelloPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundHelloPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.login.ServerboundHelloPacket");

    @FieldGetter(name = "name")
    String getName(Object target);

    @FieldGetter(name = "profileId", activeIf = "min_version=1.20.2")
    UUID getProfileId(Object target);

    @FieldGetter(name = "profileId", activeIf = "max_version=1.20.1")
    Optional<UUID> getProfileId$legacy(Object target);
}
