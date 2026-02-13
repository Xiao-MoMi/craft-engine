package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom.CustomPacketPayloadProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Optional;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket"})
public interface ServerboundCustomPayloadPacketProxy {
    ServerboundCustomPayloadPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundCustomPayloadPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket", "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket");

    @FieldGetter(name = "payload", activeIf = "min_version=1.20.2")
    Object getPayload(Object target);

    @FieldGetter(name = "data", activeIf = "max_version=1.20.1")
    Object getData(Object target);

    @ReflectionProxy(name = "net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket$UnknownPayload", activeIf = "max_version=1.20.4 && min_version=1.20.2")
    interface UnknownPayloadProxy extends CustomPacketPayloadProxy {
        UnknownPayloadProxy INSTANCE = ASMProxyFactory.create(UnknownPayloadProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket$UnknownPayload");
        boolean PAPER_PATCH = Optional.ofNullable(SparrowClass.ofNullable(CLASS))
                .map(it -> it.getDeclaredField(FieldMatcher.named("data")))
                .map(field -> field.getType() == byte[].class)
                .orElse(false);
        SConstructor2 CONSTRUCTOR = Optional.ofNullable(SparrowClass.ofNullable(CLASS))
                .map(it -> it.getSparrowConstructor(
                        ConstructorMatcher.takeArgument(0, IdentifierProxy.CLASS)
                                .and(ConstructorMatcher.takeArgument(1, byte[].class).or(ConstructorMatcher.takeArgument(1, ByteBuf.class)))
                ))
                .map(SparrowConstructor::asm$2)
                .orElse(null);

        @FieldGetter(name = "id")
        Object getId(Object target);

        @FieldGetter(name = "data")
        Object getData(Object target);
    }
}
