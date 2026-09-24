package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.PotionIngredient", activeIf = "min_version=26.3")
public interface PotionIngredientProxy {
    PotionIngredientProxy INSTANCE = ASMProxyFactory.create(PotionIngredientProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = IngredientProxy.class) Object ingredient, Optional<?> potions);
}
