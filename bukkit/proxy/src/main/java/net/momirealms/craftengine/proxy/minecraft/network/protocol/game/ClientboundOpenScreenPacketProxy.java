package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.MenuTypeProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundOpenScreenPacket")
public interface ClientboundOpenScreenPacketProxy {
    ClientboundOpenScreenPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundOpenScreenPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundOpenScreenPacket");

    @ConstructorInvoker
    Object newInstance(int syncId,
                       @Type(clazz = MenuTypeProxy.class) Object type,
                       @Type(clazz = ComponentProxy.class) Object name);
}
