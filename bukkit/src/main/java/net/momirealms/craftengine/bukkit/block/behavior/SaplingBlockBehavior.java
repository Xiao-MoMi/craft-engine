package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class SaplingBlockBehavior extends BushBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key feature;
    private final Property<Integer> stageProperty;
    private final double boneMealSuccessChance;

    public SaplingBlockBehavior(Key feature, Property<Integer> stageProperty, List<Object> tagsCanSurviveOn, double boneMealSuccessChance) {
        super(tagsCanSurviveOn);
        this.feature = feature;
        this.stageProperty = stageProperty;
        this.boneMealSuccessChance = boneMealSuccessChance;
    }

    public Key treeFeature() {
        return feature;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world = args[1];
        Object blockPos = args[2];
        Object blockState = args[0];
        int x = (int) Reflections.field$Vec3i$x.get(blockPos);
        int y = (int) Reflections.field$Vec3i$y.get(blockPos);
        int z = (int) Reflections.field$Vec3i$z.get(blockPos);
        Object aboveBlockPos = LocationUtils.toBlockPos(x, y + 1, z);
        if ((int) Reflections.method$LevelReader$getMaxLocalRawBrightness.invoke(world, aboveBlockPos) >= 9 && (float) Reflections.method$RandomSource$nextFloat.invoke(args[3]) < (1.0f / 7.0f)) {
            increaseStage(world, blockPos, blockState, args[3]);
        }
    }

    private void increaseStage(Object world, Object blockPos, Object blockState, Object randomSource) throws Exception {
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (immutableBlockState == null || immutableBlockState.isEmpty()) return;
        int currentStage = immutableBlockState.get(this.stageProperty);
        if (currentStage != this.stageProperty.possibleValues().get(this.stageProperty.possibleValues().size() - 1)) {
            ImmutableBlockState nextStage = immutableBlockState.cycle(this.stageProperty);
            World bukkitWorld = (World) Reflections.method$Level$getCraftWorld.invoke(world);
            int x = (int) Reflections.field$Vec3i$x.get(blockPos);
            int y = (int) Reflections.field$Vec3i$y.get(blockPos);
            int z = (int) Reflections.field$Vec3i$z.get(blockPos);
            CraftEngineBlocks.place(new Location(bukkitWorld, x, y, z), nextStage, UpdateOption.UPDATE_NONE, false);
        } else {
            generateTree(world, blockPos, blockState, randomSource);
        }
    }

    private void generateTree(Object world, Object blockPos, Object blockState, Object randomSource) throws Exception {
        Object registry = Reflections.method$RegistryAccess$registryOrThrow.invoke(Reflections.instance$registryAccess, Reflections.instance$Registries$CONFIGURED_FEATURE);
        if (registry == null) return;
        @SuppressWarnings("unchecked")
        Optional<Object> holder = (Optional<Object>) Reflections.method$Registry$getHolder1.invoke(registry, FeatureUtils.createFeatureKey(treeFeature()));
        if (holder.isEmpty()) {
            CraftEngine.instance().logger().warn("Configured feature not found: " + treeFeature());
            return;
        }
        Object chunkGenerator = Reflections.method$ServerChunkCache$getGenerator.invoke(Reflections.field$ServerLevel$chunkSource.get(world));
        Object configuredFeature = Reflections.method$Holder$value.invoke(holder.get());
        Object fluidState = Reflections.method$Level$getFluidState.invoke(world, blockPos);
        Object legacyState = Reflections.method$FluidState$createLegacyBlock.invoke(fluidState);
        Reflections.method$Level$setBlock.invoke(world, blockPos, legacyState, UpdateOption.UPDATE_NONE.flags());
        if ((boolean) Reflections.method$ConfiguredFeature$place.invoke(configuredFeature, world, chunkGenerator, randomSource, blockPos)) {
            if (Reflections.method$BlockGetter$getBlockState.invoke(world, blockPos) == legacyState) {
                Reflections.method$ServerLevel$sendBlockUpdated.invoke(world, blockPos, blockState, legacyState, 2);
            }
        } else {
            // failed to place, rollback changes
            Reflections.method$Level$setBlock.invoke(blockPos, blockState, UpdateOption.UPDATE_NONE.flags());
        }
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        return RandomUtils.generateRandomDouble(0d, 1d) < this.boneMealSuccessChance;
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
        this.increaseStage(args[0], args[2], args[3], args[1]);
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String feature = (String) arguments.get("feature");
            if (feature == null) {
                throw new IllegalArgumentException("feature is null");
            }
            Property<Integer> stageProperty = (Property<Integer>) block.getProperty("stage");
            if (stageProperty == null) {
                throw new IllegalArgumentException("stage property not set for sapling");
            }
            double boneMealSuccessChance = MiscUtils.getAsDouble(arguments.getOrDefault("bone-meal-success-chance", 0.45));
            if (arguments.containsKey("tags")) {
                return new SaplingBlockBehavior(Key.of(feature), stageProperty, MiscUtils.getAsStringList(arguments.get("tags")).stream().map(it -> BlockTags.getOrCreate(Key.of(it))).toList(), boneMealSuccessChance);
            } else {
                return new SaplingBlockBehavior(Key.of(feature), stageProperty, List.of(DIRT_TAG, FARMLAND), boneMealSuccessChance);
            }
        }
    }
}
