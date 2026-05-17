package net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Collection;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.properties.EnumProperty")
public interface EnumPropertyProxy {
    EnumPropertyProxy INSTANCE = ASMProxyFactory.create(EnumPropertyProxy.class);

    @FieldGetter(name = "values")
    Collection<Object> getValues(Object target);
}
