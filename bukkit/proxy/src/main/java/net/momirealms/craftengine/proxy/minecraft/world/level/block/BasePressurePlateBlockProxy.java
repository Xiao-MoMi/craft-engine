package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.BasePressurePlateBlock")
public interface BasePressurePlateBlockProxy extends BlockProxy {
    BasePressurePlateBlockProxy INSTANCE = ASMProxyFactory.create(BasePressurePlateBlockProxy.class);

    @FieldGetter(name = "TOUCH_AABB", isStatic = true)
    Object getTouchAABB();
}
