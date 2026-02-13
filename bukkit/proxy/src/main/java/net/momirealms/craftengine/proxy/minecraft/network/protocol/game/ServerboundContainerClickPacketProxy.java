package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundContainerClickPacket")
public interface ServerboundContainerClickPacketProxy extends PacketProxy {
    ServerboundContainerClickPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundContainerClickPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundContainerClickPacket");

    @FieldGetter(name = "carriedItem")
    Object getCarriedItem(Object target);

    @FieldSetter(name = "carriedItem")
    void setCarriedItem(Object target, Object carriedItem);

    @FieldGetter(name = "changedSlots")
    Int2ObjectMap<Object> getChangedSlots(Object target);

    @FieldSetter(name = "changedSlots")
    void setChangedSlots(Object target, Int2ObjectMap<?> changedSlots);
}
