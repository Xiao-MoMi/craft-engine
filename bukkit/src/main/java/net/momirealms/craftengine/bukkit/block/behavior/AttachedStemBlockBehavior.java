package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class AttachedStemBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Property<HorizontalDirection> facingProperty;
    private final Key fruit;
    private final Key stem;

    public AttachedStemBlockBehavior(CustomBlock customBlock,
                                     Property<HorizontalDirection> facingProperty,
                                     Key fruit,
                                     Key stem) {
        super(customBlock);
        this.facingProperty = facingProperty;
        this.fruit = fruit;
        this.stem = stem;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (VersionHelper.isOrAbove1_20_5() ? args[1] : args[3]).equals(CoreReflections.Instance.pathComputationType$AIR)
                && !FastNMS.INSTANCE.field$BlockBehavior$hasCollision(thisBlock) || (boolean) superMethod.call();
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        HorizontalDirection direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]).toHorizontalDirection();
        Object neighborState = args[updateShape$neighborState];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty() || direction != optionalCustomState.get().get(this.facingProperty)) {
            return super.updateShape(thisBlock, args, superMethod);
        }
        Optional<ImmutableBlockState> optionalCustomNeighborState = BlockStateUtils.getOptionalCustomBlockState(neighborState);
        if (optionalCustomNeighborState.isPresent()) {
            ImmutableBlockState customNeighborState = optionalCustomNeighborState.get();
            if (!customNeighborState.owner().value().id().equals(this.fruit)) {
                Object stemBlock = resetStemBlock();
                if (stemBlock != null) return stemBlock;
            }
        } else {
            if (this.stem.namespace().equals("minecraft")) {
                Key neighborBlockId = BlockStateUtils.getBlockOwnerIdFromState(neighborState);
                if (!neighborBlockId.equals(this.fruit)) {
                    Object stemBlock = resetStemBlock();
                    if (stemBlock != null) return stemBlock;
                }
            } else {
                Object stemBlock = resetStemBlock();
                if (stemBlock != null) return stemBlock;
            }
        }
        return super.updateShape(thisBlock, args, superMethod);
    }

    private Object resetStemBlock() {
        Optional<CustomBlock> optionalStemBlock = BukkitBlockManager.instance().blockById(this.stem);
        if (optionalStemBlock.isPresent()) {
            CustomBlock stemBlock = optionalStemBlock.get();
            IntegerProperty ageProperty = (IntegerProperty) stemBlock.getProperty("age");
            if (ageProperty == null) return stemBlock.defaultState().customBlockState().literalObject();
            return stemBlock.defaultState().with(ageProperty, ageProperty.max).customBlockState().literalObject();
        }
        return null;
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {

            @SuppressWarnings("unchecked")
            Property<HorizontalDirection> facingProperty = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.attached_stem.missing_facing");
            Key fruit = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("fruit"), "warning.config.block.behavior.attached_stem.missing_fruit"));
            Key stem = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("stem"), "warning.config.block.behavior.attached_stem.missing_stem"));
            return new AttachedStemBlockBehavior(block, facingProperty, fruit, stem);
        }
    }
}
