package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundRenameItemPacket")
public interface ServerboundRenameItemPacketProxy {
    ServerboundRenameItemPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundRenameItemPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundRenameItemPacket");

    @FieldGetter(name = "name")
    String getName(Object target);

    @FieldSetter(name = "name")
    void setName(Object target, String name);
}
