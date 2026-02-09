package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import com.mojang.datafixers.util.Pair;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket")
public interface ClientboundSetEquipmentPacketProxy {
    ClientboundSetEquipmentPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetEquipmentPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int entityId, List<Pair<?, ?>> equipmentList);
}
