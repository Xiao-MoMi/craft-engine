package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundPlayerActionPacket")
public interface ServerboundPlayerActionPacketProxy extends PacketProxy {
    ServerboundPlayerActionPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundPlayerActionPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundPlayerActionPacket");

    @FieldGetter(name = "pos")
    Object getPos(Object target);

    @FieldGetter(name = "action")
    Object getAction(Object target);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action")
    interface ActionProxy {
        ActionProxy INSTANCE = ASMProxyFactory.create(ActionProxy.class);
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> START_DESTROY_BLOCK = VALUES[0];
        Enum<?> ABORT_DESTROY_BLOCK = VALUES[1];
        Enum<?> STOP_DESTROY_BLOCK = VALUES[2];
        Enum<?> DROP_ALL_ITEMS = VALUES[3];
        Enum<?> DROP_ITEM = VALUES[4];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }

}
