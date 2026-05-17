package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class ItemBehavior {

    public InteractionResult useOnBlock(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public void onBreakBlock(World world, Player player, BlockPos pos) {}

    @SuppressWarnings("unchecked")
    public <T> void let(Class<T> tClass, Consumer<T> consumer) {
        if (tClass.isInstance(this)) {
            consumer.accept((T) this);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getFirst(Class<T> tClass) {
        if (tClass.isInstance(this)) {
            return (T) this;
        }
        return null;
    }
}
