package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.Set;

public final class IngredientUnlockable {
    private final Recipe recipe;
    private final Requirement[] requirements;

    public IngredientUnlockable(Recipe recipe, Requirement[] requirements) {
        this.recipe = recipe;
        this.requirements = requirements;
    }

    public Key id() {
        return this.recipe.id();
    }

    public boolean canUnlock(Player player, Set<UniqueKey> obtainedItems) {
        for (Requirement requirement : this.requirements) {
            if (!requirement.test(obtainedItems)) {
                return false;
            }
        }
        if (this.recipe instanceof ConditionalRecipe conditional) {
            return conditional.canUse(PlayerOptionalContext.of(player));
        }
        return true;
    }

    public interface Requirement {

        boolean test(Set<UniqueKey> obtainedItems);
    }

    public static class Single implements Requirement {
        private final UniqueKey item;

        public Single(UniqueKey item) {
            this.item = item;
        }

        @Override
        public boolean test(Set<UniqueKey> obtainedItems) {
            return obtainedItems.contains(this.item);
        }
    }

    public static class Multiple implements Requirement {
        private final UniqueKey[] items;

        public Multiple(UniqueKey[] items) {
            this.items = items;
        }

        @Override
        public boolean test(Set<UniqueKey> obtainedItems) {
            for (UniqueKey item : this.items) {
                if (obtainedItems.contains(item)) {
                    return true;
                }
            }
            return false;
        }
    }
}
