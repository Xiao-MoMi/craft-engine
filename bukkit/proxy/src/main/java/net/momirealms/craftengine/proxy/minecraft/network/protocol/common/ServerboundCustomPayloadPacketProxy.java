package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom.CustomPacketPayloadProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket"})
public interface ServerboundCustomPayloadPacketProxy {
    ServerboundCustomPayloadPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundCustomPayloadPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket");

    @FieldGetter(name = "payload", activeIf = "min_version=1.20.2")
    Object getPayload(Object target);

    @FieldGetter(name = "data", activeIf = "max_version=1.20.1")
    Object getData(Object target);

    @ReflectionProxy(name = "net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket$UnknownPayload", activeIf = "max_version=1.20.4")
    interface UnknownPayloadProxy extends CustomPacketPayloadProxy {
        Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket$UnknownPayload");

        @FieldGetter(name = "data")
        ByteBuf getData(Object target);
    }
}
