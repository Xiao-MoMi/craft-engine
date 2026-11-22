package net.momirealms.craftengine.proxy.minecraft.network.protocol;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.PacketFlow")
public interface PacketFlowProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.PacketFlow");
}
