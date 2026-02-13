package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.DyeableLeatherItem", activeIf = "max_version=1.20.4")
public interface DyeableLeatherItemProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.DyeableLeatherItem");
}
