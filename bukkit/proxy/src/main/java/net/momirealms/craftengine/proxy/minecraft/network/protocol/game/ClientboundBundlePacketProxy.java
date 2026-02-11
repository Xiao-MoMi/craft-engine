package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.BundlePacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBundlePacket")
public interface ClientboundBundlePacketProxy extends BundlePacketProxy {
    ClientboundBundlePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundBundlePacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundBundlePacket");

    @ConstructorInvoker
    Object newInstance(Iterable<?> packets);
}
