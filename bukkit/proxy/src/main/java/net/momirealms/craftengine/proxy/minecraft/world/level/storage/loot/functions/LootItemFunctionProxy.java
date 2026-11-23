package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.functions;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.functions.LootItemFunction", activeIf = "max_version=1.20.1")
public interface LootItemFunctionProxy {
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.functions.LootItemFunction");
}
