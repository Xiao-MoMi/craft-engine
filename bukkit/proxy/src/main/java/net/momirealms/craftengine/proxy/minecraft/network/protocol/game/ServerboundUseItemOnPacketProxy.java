package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.InteractionHandProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundUseItemOnPacket")
public interface ServerboundUseItemOnPacketProxy {
    ServerboundUseItemOnPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundUseItemOnPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundUseItemOnPacket");

    @FieldSetter(name = "timestamp")
    void setTimestamp(Object packet, long timestamp);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = InteractionHandProxy.class) Object hand,
                       @Type(clazz = BlockHitResultProxy.class) Object hitResult,
                       int sequence);
}