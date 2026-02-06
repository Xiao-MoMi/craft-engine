package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.pattern;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.pattern.BlockInWorld")
public interface BlockInWorldProxy {
    BlockInWorldProxy INSTANCE = ASMProxyFactory.create(BlockInWorldProxy.class);

    @FieldGetter(name = "state")
    Object getState(Object target);

    @FieldSetter(name = "state")
    void setState(Object target, Object value);
}
