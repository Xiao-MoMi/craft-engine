package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.predicates;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.predicates.LootItemCondition", activeIf = "max_version=1.20.1")
public interface LootItemConditionProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.predicates.LootItemCondition");
}
