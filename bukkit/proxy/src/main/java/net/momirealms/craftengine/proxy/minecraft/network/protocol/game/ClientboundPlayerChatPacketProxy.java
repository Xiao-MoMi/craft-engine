package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerChatPacket")
public interface ClientboundPlayerChatPacketProxy extends PacketProxy {
    ClientboundPlayerChatPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundPlayerChatPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundPlayerChatPacket");

    @FieldGetter(name = "unsignedContent")
    Object getUnsignedContent(Object target);

    @FieldGetter(name = "body")
    Object getBody(Object target);

    @FieldGetter(name = "chatType")
    Object getChatType(Object target);

    @FieldGetter(name = "sender")
    UUID getSender(Object target);
}
