package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.entity.TintSourceBlockEntityController;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;

public final class TintSourceBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<TintSourceBlockBehavior> FACTORY = new Factory();
    public final boolean dropItem;
    public final String customDataKey;
    private int controllerId;

    public TintSourceBlockBehavior(BlockDefinition blockDefinition, boolean dropItem, String customDataKey) {
        super(blockDefinition);
        this.dropItem = dropItem;
        this.customDataKey = customDataKey;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new TintSourceBlockEntityController(blockEntity, this);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    @Override
    public Item itemToPickup(World world, BlockPos pos, ImmutableBlockState state, Player player) {
        BlockEntity blockEntity = world.storageWorld().getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) return null;
        return blockEntity.controller.let(TintSourceBlockEntityController.class, this.controllerId, TintSourceBlockEntityController::tintSource);
    }

    private static class Factory implements BlockBehaviorFactory<TintSourceBlockBehavior> {
        private static final String[] DROP_ITEM = new String[] {"drop_item", "drop-item"};
        private static final String[] DATA_KEY = new String[] {"data_key", "data-key"};

        @Override
        public TintSourceBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new TintSourceBlockBehavior(
                    block,
                    section.getBoolean(DROP_ITEM, true),
                    section.getValue(DATA_KEY, ConfigValue::getAsNonEmptyString, "craftengine:tint_source")
            );
        }
    }
}
