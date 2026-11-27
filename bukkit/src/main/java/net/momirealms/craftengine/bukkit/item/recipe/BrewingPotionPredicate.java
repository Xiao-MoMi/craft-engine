package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.recipe.predicate.DataComponentPredicate;
import net.momirealms.craftengine.proxy.minecraft.core.component.PotionsPredicateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

record BrewingPotionPredicate(Object predicate) implements DataComponentPredicate {
    static BrewingPotionPredicate parse(JsonObject json) {
        return new BrewingPotionPredicate(PotionsPredicateProxy.INSTANCE.codec().parse(RegistryOps.JSON, json).getOrThrow());
    }

    @Override
    public boolean test(Item item) {
        return PotionsPredicateProxy.Predicate.INSTANCE.matches(this.predicate, item.minecraftItem());
    }

    @Override
    public void apply(Item item) {
        Iterable<Object> potions = PotionsPredicateProxy.INSTANCE.potions(this.predicate).orElse(null);
        if (potions != null) {
            for (Object potion : potions) {
                Object contents = PotionsPredicateProxy.Contents.INSTANCE.newInstance(potion);
                ItemStackProxy.INSTANCE.set(item.minecraftItem(), DataComponentTypes.byId(DataComponentKeys.POTION_CONTENTS), contents);
                return;
            }
        }
    }
}
