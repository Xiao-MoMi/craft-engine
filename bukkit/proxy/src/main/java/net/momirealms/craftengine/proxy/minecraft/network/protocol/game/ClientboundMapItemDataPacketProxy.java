package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.world.level.saveddata.maps.MapIdProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.saveddata.maps.MapItemSavedDataProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;
import java.util.Collection;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundMapItemDataPacket")
public interface ClientboundMapItemDataPacketProxy {
    ClientboundMapItemDataPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundMapItemDataPacketProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance$0(@Type(clazz = MapIdProxy.class) Object mapId, byte scale, boolean locked, @Nullable Collection<?> decorations, @Nullable @Type(clazz = MapItemSavedDataProxy.MapPatchProxy.class) Object colorPatch);

    @ConstructorInvoker(activeIf = "max_version=1.20.4")
    Object newInstance$1(int id, byte scale, boolean locked, @Nullable Collection<?> icons, @Nullable @Type(clazz = MapItemSavedDataProxy.MapPatchProxy.class) Object updateData);
}
