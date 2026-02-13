package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket")
public interface ClientboundUpdateAdvancementsPacketProxy {
    ClientboundUpdateAdvancementsPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundUpdateAdvancementsPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");

    @ConstructorInvoker(activeIf = "min_version=1.21.5")
    Object newInstance(boolean reset,
                       Collection<Object> added,
                       Set<Object> removed,
                       Map<Object, Object> progress,
                       boolean showAdvancements);

    @ConstructorInvoker(activeIf = "max_version=1.21.4")
    Object newInstance(boolean reset,
                       Collection<Object> added,
                       Set<Object> removed,
                       Map<Object, Object> progress);
}
