package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.entity.PositionMoveRotationProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket", activeIf = "min_version=1.21.2")
public interface ClientboundEntityPositionSyncPacketProxy {
    ClientboundEntityPositionSyncPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundEntityPositionSyncPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int id, @Type(clazz = PositionMoveRotationProxy.class) Object values, boolean onGround);

    @FieldGetter(name = "id")
    int getId(Object target);

    @FieldGetter(name = "values")
    Object getValues(Object target);
}
