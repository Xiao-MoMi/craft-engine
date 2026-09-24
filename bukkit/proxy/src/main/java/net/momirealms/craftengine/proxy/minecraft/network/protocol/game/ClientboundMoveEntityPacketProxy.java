package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket")
public interface ClientboundMoveEntityPacketProxy extends PacketProxy {
    ClientboundMoveEntityPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundMoveEntityPacketProxy.class);

    @FieldGetter(name = "entityId")
    int getEntityId(Object target);

    @FieldGetter(name = "xa", activeIf = "max_version=26.2")
    short getXa(Object target);

    @FieldGetter(name = "ya", activeIf = "max_version=26.2")
    short getYa(Object target);

    @FieldGetter(name = "za", activeIf = "max_version=26.2")
    short getZa(Object target);

    @FieldGetter(name = "yRot")
    byte getYRot(Object target);

    @FieldGetter(name = "xRot")
    byte getXRot(Object target);

    @FieldGetter(name = "onGround")
    boolean getOnGround(Object target);

    @FieldGetter(name = "hasRot")
    boolean getHasRot(Object target);

    @FieldGetter(name = "hasPos")
    boolean getHasPos(Object target);

    @FieldSetter(name = "entityId")
    void setEntityId(Object target, int entityId);

    @FieldSetter(name = "xa", activeIf = "max_version=26.2")
    void setXa(Object target, short xa);

    @FieldSetter(name = "ya", activeIf = "max_version=26.2")
    void setYa(Object target, short ya);

    @FieldSetter(name = "za", activeIf = "max_version=26.2")
    void setZa(Object target, short za);

    @FieldSetter(name = "yRot")
    void setYRot(Object target, byte yRot);

    @FieldSetter(name = "xRot")
    void setXRot(Object target, byte xRot);

    @FieldSetter(name = "onGround")
    void setOnGround(Object target, boolean onGround);

    @FieldSetter(name = "hasRot")
    void setHasRot(Object target, boolean hasRot);

    @FieldSetter(name = "hasPos")
    void setHasPos(Object target, boolean hasPos);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$PosRot")
    interface PosRotProxy {
        PosRotProxy INSTANCE = ASMProxyFactory.create(PosRotProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$PosRot");

        @ConstructorInvoker(activeIf = "max_version=26.2")
        default Object newInstance(int entityId, short xa, short ya, short za, byte yRot, byte xRot, boolean onGround) {
            return newInstance(entityId, VecDeltaProxy.LinearProxy.INSTANCE.newInstance(xa, ya, za), yRot, xRot, onGround);
        }

        @ConstructorInvoker(activeIf = "min_version=26.3")
        Object newInstance(int entityId, @net.momirealms.sparrow.reflection.proxy.annotation.Type(clazz = VecDeltaProxy.class) Object delta, byte yRot, byte xRot, boolean onGround);
    }

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Pos")
    interface PosProxy {
        PosProxy INSTANCE = ASMProxyFactory.create(PosProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Pos");

        @ConstructorInvoker(activeIf = "max_version=26.2")
        default Object newInstance(int entityId, short deltaX, short deltaY, short deltaZ, boolean onGround) {
            return newInstance(entityId, VecDeltaProxy.LinearProxy.INSTANCE.newInstance(deltaX, deltaY, deltaZ), onGround);
        }

        @ConstructorInvoker(activeIf = "min_version=26.3")
        Object newInstance(int entityId, @net.momirealms.sparrow.reflection.proxy.annotation.Type(clazz = VecDeltaProxy.class) Object delta, boolean onGround);
    }
}
