package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket")
public interface ClientboundMoveEntityPacketProxy {
    ClientboundMoveEntityPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundMoveEntityPacketProxy.class);

    @FieldGetter(name = "entityId")
    int getEntityId(Object target);

    @FieldGetter(name = "xa")
    short getXa(Object target);

    @FieldGetter(name = "ya")
    short getYa(Object target);

    @FieldGetter(name = "za")
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

    @FieldSetter(name = "xa")
    void setXa(Object target, short xa);

    @FieldSetter(name = "ya")
    void setYa(Object target, short ya);

    @FieldSetter(name = "za")
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

        @ConstructorInvoker
        Object newInstance(int entityId, short xa, short ya, short za, byte yRot, byte xRot, boolean onGround);
    }
}
