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

    @ConstructorInvoker(activeIf = "min_version=1.21.5 && max_version=26.2")
    default Object newInstance(boolean reset, Collection<Object> added, Set<Object> removed, Map<Object, Object> progress, boolean showAdvancements) {
        return newInstance26_3(reset, added.stream().map(a -> PositionedAdvancementProxy.INSTANCE.newInstance(a, 0, 0)).toList(), removed, progress, showAdvancements);
    }

    @ConstructorInvoker(activeIf = "min_version=26.3")
    Object newInstance26_3(boolean reset, java.util.List<Object> added, Set<Object> removed, Map<Object, Object> progress, boolean showAdvancements);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket$PositionedAdvancement", activeIf = "min_version=26.3")
    interface PositionedAdvancementProxy {
        PositionedAdvancementProxy INSTANCE = ASMProxyFactory.create(PositionedAdvancementProxy.class);
        @ConstructorInvoker
        Object newInstance(@net.momirealms.sparrow.reflection.proxy.annotation.Type(clazz = net.momirealms.craftengine.proxy.minecraft.advancements.AdvancementHolderProxy.class) Object advancement, float x, float y);
    }

    @ConstructorInvoker(activeIf = "max_version=1.21.4")
    Object newInstance(boolean reset,
                       Collection<Object> added,
                       Set<Object> removed,
                       Map<Object, Object> progress);
}
