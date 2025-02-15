package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class BoneMealBehavior extends ItemBehavior {
    public static final BoneMealBehavior INSTANCE = new BoneMealBehavior();

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return super.useOnBlock(context);
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Key id, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
