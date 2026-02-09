package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.saveddata.maps.MapIdProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;

@ReflectionProxy(name = "net.minecraft.world.item.MapItem")
public interface MapItemProxy {
    MapItemProxy INSTANCE = ASMProxyFactory.create(MapItemProxy.class);

    @MethodInvoker(name = "getSavedData", activeIf = "min_version=1.20.5", isStatic = true)
    Object getSavedData$0(@Nullable @Type(clazz = MapIdProxy.class) Object mapId, @Type(clazz = LevelProxy.class) Object level);

    @MethodInvoker(name = "getSavedData", activeIf = "max_version=1.20.4", isStatic = true)
    Object getSavedData$1(@Nullable Integer mapId, @Type(clazz = LevelProxy.class) Object level);

    @MethodInvoker(name = "getMapId", activeIf = "max_version=1.20.4", isStatic = true)
    Integer getMapId(@Type(clazz = MapItemProxy.class) Object mapItem);
}
