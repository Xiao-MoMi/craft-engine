package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.BrewingRecipe", activeIf = "min_version=26.3")
public interface BrewingRecipeProxy {
    BrewingRecipeProxy INSTANCE = ASMProxyFactory.create(BrewingRecipeProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = PotionIngredientProxy.class) Object input,
                       @Type(clazz = PotionIngredientProxy.class) Object reagent,
                       @Type(clazz = ItemStackTemplateProxy.class) Object output);
}
