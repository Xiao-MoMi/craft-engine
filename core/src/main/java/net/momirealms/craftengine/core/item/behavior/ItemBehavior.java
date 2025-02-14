package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.world.World;

public abstract class ItemBehavior {

    public InteractionResult useOnBlock(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public InteractionResult use(World world, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}
