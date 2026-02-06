package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.SnowLayerBlock")
public interface SnowLayerBlockProxy extends BlockProxy {
    SnowLayerBlockProxy INSTANCE = ASMProxyFactory.create(SnowLayerBlockProxy.class);

    @FieldGetter(name = "LAYERS", isStatic = true)
    Object getLayersProperty();
}
