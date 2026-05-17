package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.recipe.RecipeRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeProxy;

import java.util.Collection;
import java.util.Map;

public final class RecipeRegistry1_20_5 implements RecipeRegistry {
    private Multimap<Object, Object> byType = null;
    private Map<Object, Object> byName = null;

    public RecipeRegistry1_20_5() {
    }

    @Override
    public void prepareRegistration() {
        this.byType = LinkedHashMultimap.create(RecipeManagerProxy.INSTANCE.getByType(BukkitRecipeManager.minecraftRecipeManager()));
        this.byName = Maps.newHashMap(RecipeManagerProxy.INSTANCE.getByName(BukkitRecipeManager.minecraftRecipeManager()));
    }

    @Override
    public void register(Key id, Object recipe) {
        Object resourceLocation = KeyUtils.toIdentifier(id);
        Object recipeHolder = RecipeHolderProxy.INSTANCE.newInstance$1(resourceLocation, recipe);
        Collection<Object> recipes = this.byType.get(RecipeProxy.INSTANCE.getType(recipe));
        recipes.add(recipeHolder);
        this.byName.put(resourceLocation, recipeHolder);
    }

    @Override
    public void unregister(Key id) {
        Object identifier = KeyUtils.toIdentifier(id);
        Object removed = this.byName.remove(identifier);
        if (removed != null) {
            this.byType.get(RecipeProxy.INSTANCE.getType(RecipeHolderProxy.INSTANCE.getValue(removed))).remove(removed);
        }
    }

    @Override
    public Object get(Key id) {
        return this.byName.get(KeyUtils.toIdentifier(id));
    }

    @Override
    public void finalizeRegistration() {
        if (this.byName != null && this.byType != null) {
            RecipeManagerProxy.INSTANCE.setByType(BukkitRecipeManager.minecraftRecipeManager(), this.byType);
            RecipeManagerProxy.INSTANCE.setByName(BukkitRecipeManager.minecraftRecipeManager(), this.byName);
        }
        this.byType = null;
        this.byName = null;
    }
}
