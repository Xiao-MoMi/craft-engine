package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Collection;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket")
public interface ClientboundUpdateAttributesPacketProxy extends PacketProxy {
    ClientboundUpdateAttributesPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundUpdateAttributesPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int entityId, Collection<?> attributes);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket$AttributeSnapshot")
    interface AttributeSnapshotProxy {
        AttributeSnapshotProxy INSTANCE = ASMProxyFactory.create(AttributeSnapshotProxy.class);

        @ConstructorInvoker(activeIf = "min_version=1.20.5")
        Object newInstance(@Type(clazz = HolderProxy.class) Object attribute,
                           double base,
                           Collection<Object> modifiers);
    }
}
