package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.BonemealableBlock;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class StemBlockBehavior extends BukkitBlockBehavior implements PathFindingBlock, BonemealableBlock, RandomTickBlock {
    public static final BlockBehaviorFactory<StemBlockBehavior> FACTORY = new Factory();
    public final IntegerProperty ageProperty;
    public final Key fruit;
    public final Key attachedStem;
    public final int minGrowLight;
    public final List<Object> tagsCanSurviveOn;
    public final LazyReference<Set<Object>> blockStatesCanSurviveOn;

    private StemBlockBehavior(BlockDefinition blockDefinition,
                              IntegerProperty ageProperty,
                              Key fruit,
                              Key attachedStem,
                              int minGrowLight,
                              List<Object> tags,
                              LazyReference<Set<Object>> blockStates) {
        super(blockDefinition);
        this.ageProperty = ageProperty;
        this.fruit = fruit;
        this.attachedStem = attachedStem;
        this.minGrowLight = minGrowLight;
        this.tagsCanSurviveOn = tags;
        this.blockStatesCanSurviveOn = blockStates;
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        return (VersionHelper.isOrAbove1_20_5 ? args[1] : args[3]).equals(PathComputationTypeProxy.AIR)
                && !BlockBehaviourProxy.INSTANCE.hasCollision(thisBlock) || super.isPathFindable(thisBlock, args);
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        if (CropBlockBehavior.getRawBrightness(level, pos) < this.minGrowLight) return;
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState == null || customState.isEmpty()) return;
        int age = customState.get(ageProperty);
        if (age < ageProperty.max) {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, customState.with(ageProperty, age + 1).customBlockState().minecraftState(), 2);
            return;
        }
        Object randomDirection = DirectionProxy.VALUES[RandomUtils.generateRandomInt(2, 6)];
        Object blockPos = BlockPosProxy.INSTANCE.relative(pos, randomDirection);
        if (!BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isAir(BlockGetterProxy.INSTANCE.getBlockState(level, blockPos)))
            return;
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, BlockPosProxy.INSTANCE.relative(blockPos, DirectionProxy.DOWN));
        if (mayPlaceFruit(blockState)) {
            Optional<BlockDefinition> optionalFruit = BukkitBlockManager.instance().blockById(this.fruit);
            Object fruitState = null;
            if (optionalFruit.isPresent()) {
                fruitState = optionalFruit.get().defaultState().customBlockState().minecraftState();
            } else if (fruit.namespace().equals("minecraft")) {
                fruitState = BlockProxy.INSTANCE.getDefaultBlockState(RegistryUtils.getRegistryValue(
                        BuiltInRegistriesProxy.BLOCK,
                        IdentifierProxy.INSTANCE.newInstance("minecraft", fruit.value())
                ));
            }
            Optional<BlockDefinition> optionalAttachedStem = BukkitBlockManager.instance().blockById(this.attachedStem);
            if (fruitState == null || optionalAttachedStem.isEmpty()) return;
            BlockDefinition attachedStem = optionalAttachedStem.get();
            @SuppressWarnings("unchecked")
            Property<Direction> facing = (Property<Direction>) attachedStem.getProperty("facing");
            if (facing == null) return;
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, fruitState, UpdateFlags.UPDATE_ALL);
            LevelWriterProxy.INSTANCE.setBlock(level, pos, attachedStem.defaultState().with(facing, DirectionUtils.fromNMSDirection(randomDirection)).customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
        }
    }

    @Override
    public boolean isValidBonemealTarget(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[2]).orElse(null);
        if (state == null || state.isEmpty()) return false;
        return state.get(ageProperty) != ageProperty.max;
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[3]).orElse(null);
        if (state == null || state.isEmpty()) return;
        int min = Math.min(7, state.get(ageProperty) + RandomUtils.generateRandomInt(Math.min(ageProperty.min + 2, ageProperty.max), Math.min(ageProperty.max - 2, ageProperty.max)));
        Object blockState = state.with(ageProperty, min).customBlockState().minecraftState();
        LevelWriterProxy.INSTANCE.setBlock(args[0], args[2], blockState, 2);
        if (min >= ageProperty.max) {
            BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.randomTick(blockState, args[0], args[2], LevelProxy.INSTANCE.getRandom(args[0]));
        }
    }

    private boolean mayPlaceFruit(Object belowState) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(belowState, tag)) {
                return true;
            }
        }
        if (this.blockStatesCanSurviveOn.get().contains(belowState)) {
            return true;
        }
        return false;
    }

    private static class Factory implements BlockBehaviorFactory<StemBlockBehavior> {
        private static final String[] ATTACHED_STEM = new String[]{"attached_stem", "attached-stem"};
        private static final String[] LIGHT_REQUIREMENT = new String[]{"light_requirement", "light-requirement"};

        @Override
        public StemBlockBehavior create(BlockDefinition block, ConfigSection section) {
            AbstractCanSurviveBlockBehavior.TagsAndState data = AbstractCanSurviveBlockBehavior.readTagsAndState(section, "fruit_bottom");
            return new StemBlockBehavior(
                    block,
                    (IntegerProperty) BlockBehaviorFactory.getProperty(section.path(), block, "age", Integer.class),
                    section.getNonNullIdentifier("fruit"),
                    section.getNonNullIdentifier(ATTACHED_STEM),
                    section.getInt(LIGHT_REQUIREMENT, 0),
                    data.tags(),
                    data.blockStates()
            );
        }
    }
}
