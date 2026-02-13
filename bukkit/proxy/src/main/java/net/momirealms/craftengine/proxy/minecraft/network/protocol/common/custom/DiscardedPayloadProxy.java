package net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.common.custom.DiscardedPayload", activeIf = "min_version=1.20.2")
public interface DiscardedPayloadProxy extends CustomPacketPayloadProxy {
    DiscardedPayloadProxy INSTANCE = ASMProxyFactory.create(DiscardedPayloadProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.custom.DiscardedPayload");

    @FieldGetter(name = "id")
    Object getId(Object target);

    @FieldGetter(name = "data", activeIf = "min_version=1.20.5")
    byte[] getData(Object target);
}
