package net.momirealms.craftengine.proxy.minecraft.world.item.crafting;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.item.crafting.RecipeType")
public interface RecipeTypeProxy {
    RecipeTypeProxy INSTANCE = ASMProxyFactory.create(RecipeTypeProxy.class);
    Object CRAFTING = INSTANCE.getCrafting();
    Object SMELTING = INSTANCE.getSmelting();
    Object BLASTING = INSTANCE.getBlasting();
    Object SMOKING = INSTANCE.getSmoking();
    Object CAMPFIRE_COOKING = INSTANCE.getCampfireCooking();
    Object STONECUTTING = INSTANCE.getStonecutting();
    Object SMITHING = INSTANCE.getSmithing();

    @FieldGetter(name = "CRAFTING")
    Object getCrafting();

    @FieldGetter(name = "SMELTING")
    Object getSmelting();

    @FieldGetter(name = "BLASTING")
    Object getBlasting();

    @FieldGetter(name = "SMOKING")
    Object getSmoking();

    @FieldGetter(name = "CAMPFIRE_COOKING")
    Object getCampfireCooking();

    @FieldGetter(name = "STONECUTTING")
    Object getStonecutting();

    @FieldGetter(name = "SMITHING")
    Object getSmithing();
}
