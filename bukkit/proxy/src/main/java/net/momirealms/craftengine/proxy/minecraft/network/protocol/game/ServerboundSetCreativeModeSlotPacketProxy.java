package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket")
public interface ServerboundSetCreativeModeSlotPacketProxy {
    ServerboundSetCreativeModeSlotPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundSetCreativeModeSlotPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket");

    @FieldGetter(name = "slotNum", activeIf = "min_version=1.20.5")
    short getSlotNum(Object target);

    @FieldGetter(name = "slotNum", activeIf = "max_version=1.20.4")
    int getSlotNum$legacy(Object target);

    @FieldGetter(name = "itemStack")
    Object getItemStack(Object target);
}
