package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket")
public interface ClientboundBlockDestructionPacketProxy extends PacketProxy {
    ClientboundBlockDestructionPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundBlockDestructionPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int entityId, @Type(clazz = BlockPosProxy.class) Object pos, int progress);
}
