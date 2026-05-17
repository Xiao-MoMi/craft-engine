package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.recipe.RecipeRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeProxy;

import java.util.Map;

public final class RecipeRegistry1_20_2 implements RecipeRegistry {
    private Map<Object, Object> byType = null;
    private Map<Object, Object> byName = null;

    public RecipeRegistry1_20_2() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void prepareRegistration() {
        this.byType = Maps.newHashMap();
        Map<Object, Object> previousByType = RecipeManagerProxy.INSTANCE.getByType$legacy(BukkitRecipeManager.minecraftRecipeManager());
        for (Map.Entry<Object, Object> entry : previousByType.entrySet()) {
            Object recipeType = entry.getKey();
            Map<Object, Object> innerRecipes = (Map<Object, Object>) entry.getValue();
            this.byType.put(recipeType, new Object2ObjectLinkedOpenHashMap<>(innerRecipes));
        }
        this.byName = Maps.newHashMap(RecipeManagerProxy.INSTANCE.getByName(BukkitRecipeManager.minecraftRecipeManager()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(Key id, Object recipe) {
        Object resourceLocation = KeyUtils.toIdentifier(id);
        Object2ObjectLinkedOpenHashMap<Object, Object> innerRecipes = (Object2ObjectLinkedOpenHashMap<Object, Object>) this.byType.get(RecipeProxy.INSTANCE.getType(recipe));
        if (!this.byName.containsKey(resourceLocation) && !innerRecipes.containsKey(resourceLocation)) {
            Object recipeHolder = RecipeHolderProxy.INSTANCE.newInstance$1(resourceLocation, recipe);
            innerRecipes.putAndMoveToFirst(resourceLocation, recipeHolder);
            this.byName.put(resourceLocation, recipeHolder);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unregister(Key id) {
        Object identifier = KeyUtils.toIdentifier(id);
        Object removed = this.byName.remove(identifier);
        if (removed != null) {
            Object2ObjectLinkedOpenHashMap<Object, Object> innerRecipes = (Object2ObjectLinkedOpenHashMap<Object, Object>) this.byType.get(RecipeProxy.INSTANCE.getType(RecipeHolderProxy.INSTANCE.getValue(removed)));
            innerRecipes.remove(identifier);
        }
    }

    @Override
    public Object get(Key id) {
        return this.byName.get(KeyUtils.toIdentifier(id));
    }

    @Override
    public void finalizeRegistration() {
        if (this.byName != null && this.byType != null) {
            RecipeManagerProxy.INSTANCE.setByType$legacy(BukkitRecipeManager.minecraftRecipeManager(), this.byType);
            RecipeManagerProxy.INSTANCE.setByName(BukkitRecipeManager.minecraftRecipeManager(), this.byName);
        }
        this.byType = null;
        this.byName = null;
    }
}
