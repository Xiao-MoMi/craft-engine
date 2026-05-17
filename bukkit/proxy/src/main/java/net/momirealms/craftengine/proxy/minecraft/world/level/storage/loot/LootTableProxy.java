package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.LootTable")
public interface LootTableProxy {
    LootTableProxy INSTANCE = ASMProxyFactory.create(LootTableProxy.class);
    Object EMPTY = INSTANCE.getEmptyLootTable();

    @FieldGetter(isStatic = true, name = "EMPTY")
    Object getEmptyLootTable();

    @MethodInvoker(name = "getRandomItems")
    List<Object> getRandomItems(Object target, @Type(clazz = LootParamsProxy.class) Object params);
}
