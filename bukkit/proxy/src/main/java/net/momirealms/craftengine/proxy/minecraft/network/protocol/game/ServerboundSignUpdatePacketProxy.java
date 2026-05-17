package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundSignUpdatePacket")
public interface ServerboundSignUpdatePacketProxy {
    ServerboundSignUpdatePacketProxy INSTANCE = ASMProxyFactory.create(ServerboundSignUpdatePacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundSignUpdatePacket");

    @FieldGetter(name = "lines")
    String[] getLines(Object target);
}
