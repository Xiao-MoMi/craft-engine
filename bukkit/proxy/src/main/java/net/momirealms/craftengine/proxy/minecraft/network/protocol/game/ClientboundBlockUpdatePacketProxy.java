package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket")
public interface ClientboundBlockUpdatePacketProxy {
    ClientboundBlockUpdatePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundBlockUpdatePacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = BlockGetterProxy.class) Object world,
                       @Type(clazz = BlockPosProxy.class) Object blockPos);
}
