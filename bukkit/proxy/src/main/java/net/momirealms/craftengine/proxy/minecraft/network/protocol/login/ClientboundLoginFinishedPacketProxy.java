package net.momirealms.craftengine.proxy.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket", "net.minecraft.network.protocol.login.ClientboundGameProfilePacket"})
public interface ClientboundLoginFinishedPacketProxy extends PacketProxy {
    ClientboundLoginFinishedPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLoginFinishedPacketProxy.class);

    @FieldGetter(name = "gameProfile")
    GameProfile getGameProfile(Object target);
}
