package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.advancements.AdvancementRewards")
public interface AdvancementRewardsProxy {
    AdvancementRewardsProxy INSTANCE = ASMProxyFactory.create(AdvancementRewardsProxy.class);
    Object EMPTY = INSTANCE.getEmpty();

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEmpty();
}
