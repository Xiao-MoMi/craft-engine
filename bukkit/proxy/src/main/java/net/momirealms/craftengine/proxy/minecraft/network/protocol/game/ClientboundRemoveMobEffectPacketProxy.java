package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket")
public interface ClientboundRemoveMobEffectPacketProxy {
    ClientboundRemoveMobEffectPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundRemoveMobEffectPacketProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance(int entityId, @Type(clazz = HolderProxy.class) Object effect);

    @ConstructorInvoker(activeIf = "max_version=1.20.4")
    Object newInstance$legacy(int entityId, @Type(clazz = MobEffectProxy.class) Object effect);
}
