package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ChunkPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.lighting.LevelLightEngineProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundLightUpdatePacket")
public interface ClientboundLightUpdatePacketProxy extends PacketProxy {
    ClientboundLightUpdatePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLightUpdatePacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ChunkPosProxy.class) Object chunkPos, @Type(clazz = LevelLightEngineProxy.class) Object lightEngine, @Nullable BitSet skyLight, @Nullable BitSet blockLight);
}
