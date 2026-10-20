package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.BowItem")
public interface BowItemProxy {
    BowItemProxy INSTANCE = ASMProxyFactory.create(BowItemProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.BowItem");

    @FieldGetter(name = "MAX_DRAW_DURATION", isStatic = true)
    int getMaxDrawDuration();
}
