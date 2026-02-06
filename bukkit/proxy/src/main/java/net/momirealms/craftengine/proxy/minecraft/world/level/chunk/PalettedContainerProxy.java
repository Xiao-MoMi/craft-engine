package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.PalettedContainer")
public interface PalettedContainerProxy {
    PalettedContainerProxy INSTANCE = ASMProxyFactory.create(PalettedContainerProxy.class);

    @FieldSetter(name = "data")
    void setData(Object target, Object data);

    @FieldGetter(name = "data")
    Object getData(Object target);

    @ReflectionProxy(name = "net.minecraft.world.level.chunk.PalettedContainer$Data")
    interface DataProxy {
        DataProxy INSTANCE = ASMProxyFactory.create(DataProxy.class);

        @FieldGetter(name = "palette")
        Object getPalette(Object target);
    }
}
