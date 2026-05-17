package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.recipe.RecipeRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeMapProxy;

import java.util.Map;

public final class RecipeRegistry1_21_2 implements RecipeRegistry {
    private Object mirrorRecipeMap = null;

    public RecipeRegistry1_21_2() {
    }

    @Override
    public void prepareRegistration() {
        Object previousRecipeMap = RecipeManagerProxy.INSTANCE.getRecipes(BukkitRecipeManager.minecraftRecipeManager());
        Multimap<Object, Object> byType = LinkedHashMultimap.create(RecipeMapProxy.INSTANCE.getByType(previousRecipeMap));
        Map<Object, Object> byKey = Maps.newHashMap(RecipeMapProxy.INSTANCE.getByKey(previousRecipeMap));
        this.mirrorRecipeMap = RecipeMapProxy.INSTANCE.newInstance(byType, byKey);
    }

    @Override
    public void register(Key id, Object recipe) {
        Object resourceKey = ResourceKeyProxy.INSTANCE.create(RegistriesProxy.RECIPE, KeyUtils.toIdentifier(id));
        Object recipeHolder = RecipeHolderProxy.INSTANCE.newInstance$0(resourceKey, recipe);
        RecipeMapProxy.INSTANCE.addRecipe(this.mirrorRecipeMap, recipeHolder);
    }

    @Override
    public void unregister(Key id) {
        Object resourceKey = ResourceKeyProxy.INSTANCE.create(RegistriesProxy.RECIPE, KeyUtils.toIdentifier(id));
        RecipeMapProxy.INSTANCE.removeRecipe(this.mirrorRecipeMap, resourceKey);
    }

    @Override
    public Object get(Key id) {
        Object resourceKey = ResourceKeyProxy.INSTANCE.create(RegistriesProxy.RECIPE, KeyUtils.toIdentifier(id));
        return RecipeMapProxy.INSTANCE.byKey(this.mirrorRecipeMap, resourceKey);
    }

    @Override
    public void finalizeRegistration() {
        if (this.mirrorRecipeMap != null) {
            RecipeManagerProxy.INSTANCE.setRecipes(BukkitRecipeManager.minecraftRecipeManager(), this.mirrorRecipeMap);
        }
        this.mirrorRecipeMap = null;
    }
}
