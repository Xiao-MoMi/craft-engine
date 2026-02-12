package net.momirealms.craftengine.proxy.minecraft.world.item.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.component.CustomData", activeIf = "min_version=1.20.5")
public interface CustomDataProxy {
    CustomDataProxy INSTANCE = ASMProxyFactory.create(CustomDataProxy.class);

    @FieldGetter(name = "tag")
    Object getTag(Object target);
}
