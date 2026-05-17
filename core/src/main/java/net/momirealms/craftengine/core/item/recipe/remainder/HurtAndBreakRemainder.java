package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

public final class HurtAndBreakRemainder implements CraftRemainder {
    public static final CraftRemainderFactory<HurtAndBreakRemainder> FACTORY = new Factory();
    private final int amount;

    public HurtAndBreakRemainder(int amount) {
        this.amount = amount;
    }

    @Override
    public Item remainder(Key recipeId, Item item) {
        int damage = item.damage().orElse(0);
        int maxDamage = item.maxDamage();
        damage += amount;
        if (damage >= maxDamage) {
            return item.copyWithCount(0);
        } else {
            item = item.copyWithCount(1);
            item.damage(damage);
            return item;
        }
    }

    private static class Factory implements CraftRemainderFactory<HurtAndBreakRemainder> {

        @Override
        public HurtAndBreakRemainder create(ConfigSection section) {
            return new HurtAndBreakRemainder(section.getInt("damage", 1));
        }
    }
}
