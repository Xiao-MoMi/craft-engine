package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.function.Consumer;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.parameters.LootContextParamSets")
public interface LootContextParamSetsProxy {
    LootContextParamSetsProxy INSTANCE = ASMProxyFactory.create(LootContextParamSetsProxy.class);
    Object EMPTY = INSTANCE.getEmpty();
    Object BLOCK = INSTANCE.getBlock();
    Object ALL_PARAMS = INSTANCE.getAllParams();

    @FieldGetter(name = "EMPTY", isStatic = true)
    Object getEmpty();

    @FieldGetter(name = "BLOCK", isStatic = true)
    Object getBlock();

    @FieldGetter(name = "ALL_PARAMS", isStatic = true)
    Object getAllParams();

    @MethodInvoker(name = "register", isStatic = true)
    Object register(String name, Consumer<Object> constructor);

}