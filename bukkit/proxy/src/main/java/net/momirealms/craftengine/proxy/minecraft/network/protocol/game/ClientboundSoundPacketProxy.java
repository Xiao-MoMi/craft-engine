package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSoundPacket")
public interface ClientboundSoundPacketProxy extends PacketProxy {
    ClientboundSoundPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSoundPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundSoundPacket");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = HolderProxy.class) Object sound,
                       @Type(clazz = SoundSourceProxy.class) Object source,
                       double x,
                       double y,
                       double z,
                       float volume,
                       float pitch,
                       long seed);
}
