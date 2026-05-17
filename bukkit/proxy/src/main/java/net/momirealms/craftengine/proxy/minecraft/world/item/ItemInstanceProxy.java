package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.core.TypedInstanceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.ItemInstance", activeIf = "min_version=26.1")
public interface ItemInstanceProxy extends TypedInstanceProxy {
    ItemInstanceProxy INSTANCE = ASMProxyFactory.create(ItemInstanceProxy.class);
}
