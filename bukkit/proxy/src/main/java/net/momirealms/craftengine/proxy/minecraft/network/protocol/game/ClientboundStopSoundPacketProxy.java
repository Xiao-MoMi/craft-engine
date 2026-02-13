package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundStopSoundPacket")
public interface ClientboundStopSoundPacketProxy {
    ClientboundStopSoundPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundStopSoundPacketProxy.class);

    Object newInstance(@Type(clazz = IdentifierProxy.class) Object soundId,
                       @Type(clazz = SoundSourceProxy.class) Object soundSource);
}
