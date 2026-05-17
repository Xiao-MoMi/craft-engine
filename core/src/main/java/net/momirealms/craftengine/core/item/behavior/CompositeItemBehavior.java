package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public final class CompositeItemBehavior extends ItemBehavior {
    private final ItemBehavior[] behaviors;

    public CompositeItemBehavior(Collection<ItemBehavior> behaviors) {
        this.behaviors = behaviors.toArray(new ItemBehavior[0]);
    }

    @Override
    public <T> void let(Class<T> tClass, Consumer<T> consumer) {
        for (ItemBehavior behavior : this.behaviors) {
            behavior.let(tClass, consumer);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFirst(Class<T> tClass) {
        for (ItemBehavior behavior : this.behaviors) {
            if (tClass.isInstance(behavior)) {
                return (T) behavior;
            }
        }
        return null;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        for (ItemBehavior behavior : behaviors) {
            InteractionResult result = behavior.useOnBlock(context);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        for (ItemBehavior behavior : this.behaviors) {
            InteractionResult result = behavior.use(world, player, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.use(world, player, hand);
    }

    @Override
    public void onBreakBlock(World world, Player player, BlockPos pos) {
        for (ItemBehavior behavior : this.behaviors) {
            behavior.onBreakBlock(world, player, pos);
        }
    }
}
