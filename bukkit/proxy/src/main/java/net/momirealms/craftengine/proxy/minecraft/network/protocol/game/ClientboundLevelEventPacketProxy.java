package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundLevelEventPacket")
public interface ClientboundLevelEventPacketProxy extends PacketProxy {
    ClientboundLevelEventPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundLevelEventPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundLevelEventPacket");

    @ConstructorInvoker
    Object newInstance(int eventId, @Type(clazz = BlockPosProxy.class) Object pos, int data, boolean global);
}
