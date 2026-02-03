package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.Packet")
public interface PacketProxy {
    PacketProxy INSTANCE = ReflectionHelper.getProxy(PacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(PacketProxy.class);

    @MethodInvoker(name = "write", version = "<1.20.5")
    void write(Object instance, @Type(clazz = FriendlyByteBufProxy.class) Object buf);
}
