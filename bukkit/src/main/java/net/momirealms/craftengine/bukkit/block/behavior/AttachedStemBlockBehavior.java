package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;

import java.util.Optional;

public final class AttachedStemBlockBehavior extends BukkitBlockBehavior implements PathFindingBlock {
    public static final BlockBehaviorFactory<AttachedStemBlockBehavior> FACTORY = new Factory();
    public final Property<Direction> facingProperty;
    public final Key fruit;
    public final Key stem;

    private AttachedStemBlockBehavior(BlockDefinition blockDefinition,
                                      Property<Direction> facingProperty,
                                      Key fruit,
                                      Key stem) {
        super(blockDefinition);
        this.facingProperty = facingProperty;
        this.fruit = fruit;
        this.stem = stem;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        return (VersionHelper.isOrAbove1_20_5 ? args[1] : args[3]).equals(PathComputationTypeProxy.AIR)
                && !BlockBehaviourProxy.INSTANCE.hasCollision(thisBlock) || super.isPathFindable(thisBlock, args);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object state = args[0];
        Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]);
        Object neighborState = args[updateShape$neighborState];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty() || direction != optionalCustomState.get().get(this.facingProperty)) {
            return super.updateShape(thisBlock, args);
        }

        Optional<ImmutableBlockState> optionalCustomNeighborState = BlockStateUtils.getOptionalCustomBlockState(neighborState);
        Key neighborBlockId = optionalCustomNeighborState.isPresent()
                ? optionalCustomNeighborState.get().owner().value().id()
                : BlockStateUtils.getBlockOwnerIdFromState(neighborState);
        if (!neighborBlockId.equals(this.fruit)) {
            Object stemBlock = resetStemBlock();
            if (stemBlock != null) return stemBlock;
        }

        return super.updateShape(thisBlock, args);
    }

    private Object resetStemBlock() {
        Optional<BlockDefinition> optionalStemBlock = BukkitBlockManager.instance().blockById(this.stem);
        if (optionalStemBlock.isPresent()) {
            BlockDefinition stemBlock = optionalStemBlock.get();
            IntegerProperty ageProperty = (IntegerProperty) stemBlock.getProperty("age");
            if (ageProperty == null) return stemBlock.defaultState().customBlockState().minecraftState();
            return stemBlock.defaultState().with(ageProperty, ageProperty.max).customBlockState().minecraftState();
        }
        return null;
    }

    private static class Factory implements BlockBehaviorFactory<AttachedStemBlockBehavior> {

        @Override
        public AttachedStemBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new AttachedStemBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    section.getNonNullIdentifier("fruit"),
                    section.getNonNullIdentifier("stem")
            );
        }
    }
}
