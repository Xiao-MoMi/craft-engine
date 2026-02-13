package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.UUID;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundResourcePackPacket", "net.minecraft.network.protocol.game.ServerboundResourcePackPacket"})
public interface ServerboundResourcePackPacketProxy extends PacketProxy {
    ServerboundResourcePackPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundResourcePackPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.common.ServerboundResourcePackPacket", "net.minecraft.network.protocol.game.ServerboundResourcePackPacket");

    @FieldGetter(name = "id", activeIf = "min_version=1.20.3")
    UUID getId(Object target);

    @FieldGetter(name = "action")
    Object getAction(Object target);

    @ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundResourcePackPacket$Action", "net.minecraft.network.protocol.game.ServerboundResourcePackPacket$Action"})
    interface ActionProxy {
        ActionProxy INSTANCE = ASMProxyFactory.create(ActionProxy.class);
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> SUCCESSFULLY_LOADED = VALUES[0];
        Enum<?> DECLINED = VALUES[1];
        Enum<?> FAILED_DOWNLOAD = VALUES[2];
        Enum<?> ACCEPTED = VALUES[3];
        Enum<?> DOWNLOADED = VALUES.length > 4 ? VALUES[4] : null;
        Enum<?> INVALID_URL = VALUES.length > 5 ? VALUES[5] : null;
        Enum<?> FAILED_RELOAD = VALUES.length > 6 ? VALUES[6] : null;
        Enum<?> DISCARDED = VALUES.length > 7 ? VALUES[7] : null;

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }
}
