package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class DualItemBehavior extends ItemBehavior {
    private final ItemBehavior first;
    private final ItemBehavior second;

    public DualItemBehavior(ItemBehavior first, ItemBehavior second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public <T> void let(Class<T> tClass, Consumer<T> consumer) {
        this.first.let(tClass, consumer);
        this.second.let(tClass, consumer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFirst(Class<T> tClass) {
        if (tClass.isInstance(this.first)) return (T) this.first;
        if (tClass.isInstance(this.second)) return (T) this.second;
        return null;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        InteractionResult result = this.first.useOnBlock(context);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return this.second.useOnBlock(context);
    }

    @Override
    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        InteractionResult result = this.first.use(world, player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return this.second.use(world, player, hand);
    }

    @Override
    public void onBreakBlock(World world, Player player, BlockPos pos) {
        this.first.onBreakBlock(world, player, pos);
        this.second.onBreakBlock(world, player, pos);
    }
}
