package net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.custom.CustomPacketPayload", activeIf = "min_version=1.20.2")
public interface CustomPacketPayloadProxy {
    CustomPacketPayloadProxy INSTANCE = ASMProxyFactory.create(CustomPacketPayloadProxy.class);

    @MethodInvoker(name = "type", activeIf = "min_version=1.20.5")
    Object type(Object target);

    @ReflectionProxy(name = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type", activeIf = "min_version=1.20.5")
    interface TypeProxy {
        TypeProxy INSTANCE = ASMProxyFactory.create(TypeProxy.class);

        @FieldGetter(name = "id")
        Object getId(Object target);
    }
}
