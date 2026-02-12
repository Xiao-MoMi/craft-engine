package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

public abstract class ItemBehavior {

    public InteractionResult useOnBlock(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public void breakBlock(World world, Player player, BlockPos pos) {}
}
