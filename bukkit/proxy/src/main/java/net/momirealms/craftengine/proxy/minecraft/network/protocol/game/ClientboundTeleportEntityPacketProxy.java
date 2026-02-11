package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.PositionMoveRotationProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.UnsafeConstructor;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Set;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket")
public interface ClientboundTeleportEntityPacketProxy extends PacketProxy {
    ClientboundTeleportEntityPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundTeleportEntityPacketProxy.class);
    UnsafeConstructor UNSAFE_CONSTRUCTOR = new UnsafeConstructor(SparrowClass.find("net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket"));

    @ConstructorInvoker(activeIf = "min_version=1.21.2")
    Object newInstance(int id, @Type(clazz = PositionMoveRotationProxy.class) Object change, Set<Object> relatives, boolean onGround);

    @FieldGetter(name = "id")
    int getId(Object target);

    @FieldGetter(name = "onGround")
    boolean getOnGround(Object target);

    @FieldSetter(name = "id")
    void setId(Object target, int id);

    @FieldSetter(name = "onGround")
    void setOnGround(Object target, boolean onGround);

    @FieldGetter(name = "x", activeIf = "max_version=1.21.1")
    double getX(Object target);

    @FieldGetter(name = "y", activeIf = "max_version=1.21.1")
    double getY(Object target);

    @FieldGetter(name = "z", activeIf = "max_version=1.21.1")
    double getZ(Object target);

    @FieldGetter(name = "yRot", activeIf = "max_version=1.21.1")
    byte getYRot(Object target);

    @FieldGetter(name = "xRot", activeIf = "max_version=1.21.1")
    byte getXRot(Object target);

    @FieldSetter(name = "x", activeIf = "max_version=1.21.1")
    void setX(Object target, double x);

    @FieldSetter(name = "y", activeIf = "max_version=1.21.1")
    void setY(Object target, double y);

    @FieldSetter(name = "z", activeIf = "max_version=1.21.1")
    void setZ(Object target, double z);

    @FieldSetter(name = "yRot", activeIf = "max_version=1.21.1")
    void setYRot(Object target, byte yRot);

    @FieldSetter(name = "xRot", activeIf = "max_version=1.21.1")
    void setXRot(Object target, byte xRot);
}
