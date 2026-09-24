package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PositionMoveRotationProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket", activeIf = "min_version=1.21.2")
public interface ClientboundEntityPositionSyncPacketProxy extends PacketProxy {
    ClientboundEntityPositionSyncPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundEntityPositionSyncPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket");

    @ConstructorInvoker(activeIf = "max_version=26.2")
    default Object newInstance(int id, @Type(clazz = PositionMoveRotationProxy.class) Object values, boolean onGround) {
        return newInstance(id, net.momirealms.craftengine.proxy.minecraft.world.entity.PositionPathProxy.INSTANCE.of(PositionMoveRotationProxy.INSTANCE.getPosition(values)), PositionMoveRotationProxy.INSTANCE.getYRot(values), PositionMoveRotationProxy.INSTANCE.getXRot(values), onGround);
    }

    @ConstructorInvoker(activeIf = "min_version=26.3")
    Object newInstance(int id, @Type(clazz = net.momirealms.craftengine.proxy.minecraft.world.entity.PositionPathProxy.class) Object position, float yRot, float xRot, boolean onGround);

    @FieldGetter(name = "id")
    int getId(Object target);

    @FieldGetter(name = "onGround")
    boolean getOnGround(Object target);

    @FieldGetter(name = "values", activeIf = "max_version=26.2")
    Object getValues(Object target);
}
