package net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.custom.DiscardedPayload", activeIf = "min_version=1.20.2")
public interface DiscardedPayloadProxy extends CustomPacketPayloadProxy {
    DiscardedPayloadProxy INSTANCE = ASMProxyFactory.create(DiscardedPayloadProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.custom.DiscardedPayload");
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

    @ConstructorInvoker(activeIf = "max_version=1.20.4")
    Object newInstance(Object id);

    @FieldGetter(name = "data")
    Object getData(Object target);
}
