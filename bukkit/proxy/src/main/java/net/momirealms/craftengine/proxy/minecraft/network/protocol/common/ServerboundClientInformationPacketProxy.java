package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundClientInformationPacket", "net.minecraft.network.protocol.game.ServerboundClientInformationPacket"})
public interface ServerboundClientInformationPacketProxy {
    ServerboundClientInformationPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundClientInformationPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ServerboundClientInformationPacket", "net.minecraft.network.protocol.game.ServerboundClientInformationPacket");

    @FieldGetter(name = "language", activeIf = "max_version=1.20.1")
    String getLanguage(Object target);

    @FieldGetter(name = "information", activeIf = "min_version=1.20.2")
    Object getInformation(Object target);
}
