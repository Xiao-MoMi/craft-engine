package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.FireBlock")
public interface FireBlockProxy extends BaseFireBlockProxy {
    FireBlockProxy INSTANCE = ASMProxyFactory.create(FireBlockProxy.class);

    @FieldGetter(name = "igniteOdds")
    Object2IntMap<Object> getIgniteOdds(Object target);

    @FieldGetter(name = "burnOdds")
    Object2IntMap<Object> getBurnOdds(Object target);
}
