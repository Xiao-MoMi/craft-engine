package net.momirealms.craftengine.proxy.minecraft.world.level.saveddata.maps;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.saveddata.maps.MapId", activeIf = "min_version=1.20.5")
public interface MapIdProxy {
    MapIdProxy INSTANCE = ASMProxyFactory.create(MapIdProxy.class);
}
