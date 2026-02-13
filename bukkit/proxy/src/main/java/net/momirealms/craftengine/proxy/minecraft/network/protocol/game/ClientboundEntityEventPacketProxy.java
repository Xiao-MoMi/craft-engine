package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundEntityEventPacket")
public interface ClientboundEntityEventPacketProxy {
    ClientboundEntityEventPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundEntityEventPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundEntityEventPacket");

    @FieldGetter(name = "entityId")
    int getEntityId(Object target);

    @FieldGetter(name = "eventId")
    byte getEventId(Object target);

    Object newInstance(@Type(clazz = EntityProxy.class) Object entity, byte eventId);
}
