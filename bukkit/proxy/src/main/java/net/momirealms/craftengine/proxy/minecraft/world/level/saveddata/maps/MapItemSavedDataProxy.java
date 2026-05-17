package net.momirealms.craftengine.proxy.minecraft.world.level.saveddata.maps;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.saveddata.maps.MapItemSavedData")
public interface MapItemSavedDataProxy {
    MapItemSavedDataProxy INSTANCE = ASMProxyFactory.create(MapItemSavedDataProxy.class);

    @FieldGetter(name = "colors")
    byte[] getColors(Object target);

    @FieldGetter(name = "scale")
    byte getScale(Object target);

    @FieldGetter(name = "locked")
    boolean getLocked(Object target);

    @ReflectionProxy(name = "net.minecraft.world.level.saveddata.maps.MapItemSavedData$MapPatch")
    interface MapPatchProxy {
        MapPatchProxy INSTANCE = ASMProxyFactory.create(MapPatchProxy.class);

        @ConstructorInvoker
        Object newInstance(int startX, int startY, int width, int height, byte[] mapColors);
    }
}
