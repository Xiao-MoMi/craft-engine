package net.momirealms.craftengine.proxy.minecraft.network.protocol.common;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.UUID;

@ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundResourcePackPacket", "net.minecraft.network.protocol.game.ServerboundResourcePackPacket"})
public interface ServerboundResourcePackPacketProxy {
    ServerboundResourcePackPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundResourcePackPacketProxy.class);

    @FieldGetter(name = "id", activeIf = "min_version=1.20.3")
    UUID getId(Object target);

    @FieldGetter(name = "action")
    Object getAction(Object target);

    @ReflectionProxy(name = {"net.minecraft.network.protocol.common.ServerboundResourcePackPacket$Action", "net.minecraft.network.protocol.game.ServerboundResourcePackPacket$Action"})
    interface ActionProxy {
        ActionProxy INSTANCE = ASMProxyFactory.create(ActionProxy.class);
        Object[] VALUES = INSTANCE.values();
        Object SUCCESSFULLY_LOADED = VALUES[0];
        Object DECLINED = VALUES[1];
        Object FAILED_DOWNLOAD = VALUES[2];
        Object ACCEPTED = VALUES[3];
        Object DOWNLOADED = VALUES.length > 4 ? VALUES[4] : null;
        Object INVALID_URL = VALUES.length > 5 ? VALUES[5] : null;
        Object FAILED_RELOAD = VALUES.length > 6 ? VALUES[6] : null;
        Object DISCARDED = VALUES.length > 7 ? VALUES[7] : null;

        @MethodInvoker(name = "values", isStatic = true)
        Object[] values();
    }
}
