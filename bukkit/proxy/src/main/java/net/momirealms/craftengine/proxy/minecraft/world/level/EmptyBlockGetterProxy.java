package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.EmptyBlockGetter")
public interface EmptyBlockGetterProxy extends BlockGetterProxy {
    EmptyBlockGetterProxy INSTANCE = ASMProxyFactory.create(EmptyBlockGetterProxy.class);
    Object GETTER_INSTANCE = INSTANCE.getInstance();

    @FieldGetter(name = "INSTANCE", isStatic = true)
    Object getInstance();
}
