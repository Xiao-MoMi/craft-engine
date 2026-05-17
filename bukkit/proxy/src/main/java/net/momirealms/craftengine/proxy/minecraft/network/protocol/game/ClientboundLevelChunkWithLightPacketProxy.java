package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LevelLightEngineProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.BitSet;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket")
public interface ClientboundLevelChunkWithLightPacketProxy {
    ClientboundLevelChunkWithLightPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLevelChunkWithLightPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = LevelChunkProxy.class) Object chunk,
                       @Type(clazz = LevelLightEngineProxy.class) Object lightEngine,
                       BitSet skylight,
                       BitSet blockLight);
}
