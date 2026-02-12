package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.LeavesBlock")
public interface LeavesBlockProxy extends BlockProxy {
    LeavesBlockProxy INSTANCE = ASMProxyFactory.create(LeavesBlockProxy.class);
    Object DISTANCE = INSTANCE.getDistanceProperty();

    @FieldGetter(name = "DISTANCE", isStatic = true)
    Object getDistanceProperty();
}
