package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.BundlePacket")
public interface BundlePacketProxy {
    BundlePacketProxy INSTANCE = ReflectionHelper.getProxy(BundlePacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BundlePacketProxy.class);

    @FieldGetter(name = "packets")
    Iterable<?> packets(Object instance);

    @FieldSetter(name = "packets", strategy = Strategy.MH)
    void packets(Object instance, Iterable<?> packets);
}
