package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

public class SaplingBlockBehavior extends BushBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key feature;
    private final Property<Integer> stageProperty;
    private final double boneMealSuccessChance;
    private final float growSpeed;

    public SaplingBlockBehavior(CustomBlock block, Key feature, Property<Integer> stageProperty, List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn, double boneMealSuccessChance, float growSpeed) {
        super(block, tagsCanSurviveOn, blocksCansSurviveOn, customBlocksCansSurviveOn);
        this.feature = feature;
        this.stageProperty = stageProperty;
        this.boneMealSuccessChance = boneMealSuccessChance;
        this.growSpeed = growSpeed;
    }

    public Key treeFeature() {
        return feature;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world = args[1];
        Object blockPos = args[2];
        Object blockState = args[0];
        Object aboveBlockPos = LocationUtils.above(blockPos);
        if ((int) Reflections.method$LevelReader$getMaxLocalRawBrightness.invoke(world, aboveBlockPos) >= 9 && (float) RandomUtils.generateRandomFloat(0, 1) < growSpeed) {
            increaseStage(world, blockPos, blockState, args[3]);
        }
    }

    private void increaseStage(Object world, Object blockPos, Object blockState, Object randomSource) throws Exception {
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (immutableBlockState == null || immutableBlockState.isEmpty()) return;
        int currentStage = immutableBlockState.get(this.stageProperty);
        if (currentStage != this.stageProperty.possibleValues().get(this.stageProperty.possibleValues().size() - 1)) {
            ImmutableBlockState nextStage = immutableBlockState.cycle(this.stageProperty);
            World bukkitWorld = FastNMS.INSTANCE.method$Level$getCraftWorld(world);
            int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
            int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
            int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
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
        Object chunkGenerator = Reflections.method$ServerChunkCache$getGenerator.invoke(FastNMS.INSTANCE.method$ServerLevel$getChunkSource(world));
        Object configuredFeature = Reflections.method$Holder$value.invoke(holder.get());
        Object fluidState = Reflections.method$Level$getFluidState.invoke(world, blockPos);
        Object legacyState = Reflections.method$FluidState$createLegacyBlock.invoke(fluidState);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(world, blockPos, legacyState, UpdateOption.UPDATE_NONE.flags());
        if ((boolean) Reflections.method$ConfiguredFeature$place.invoke(configuredFeature, world, chunkGenerator, randomSource, blockPos)) {
            if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, blockPos) == legacyState) {
                Reflections.method$ServerLevel$sendBlockUpdated.invoke(world, blockPos, blockState, legacyState, 2);
            }
        } else {
            // failed to place, rollback changes
            FastNMS.INSTANCE.method$LevelWriter$setBlock(world, blockPos, blockState, UpdateOption.UPDATE_NONE.flags());
        }
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception {
        boolean success = RandomUtils.generateRandomDouble(0d, 1d) < this.boneMealSuccessChance;
        Object level = args[0];
        Object blockPos = args[2];
        Object blockState = args[3];
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (immutableBlockState == null || immutableBlockState.isEmpty()) {
            return false;
        }
        boolean sendParticles = false;
        Object visualState = immutableBlockState.vanillaBlockState().handle();
        Object visualStateBlock = Reflections.method$BlockStateBase$getBlock.invoke(visualState);
        if (Reflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
            boolean is = FastNMS.INSTANCE.method$BonemealableBlock$isValidBonemealTarget(visualStateBlock, level, blockPos, visualState);
            if (!is) {
                sendParticles = true;
            }
        } else {
            sendParticles = true;
        }
        if (sendParticles) {
            World world = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
            int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
            int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
            int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
            world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 15, 0.25, 0.25, 0.25);
        }
        return success;
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
            String feature = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("feature"), "warning.config.block.behavior.sapling.missing_feature");
            Property<Integer> stageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("stage"), "warning.config.block.behavior.sapling.missing_stage");
            double boneMealSuccessChance = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("bone-meal-success-chance", 0.45), "bone-meal-success-chance");
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, false);
            return new SaplingBlockBehavior(block, Key.of(feature), stageProperty, tuple.left(), tuple.mid(), tuple.right(), boneMealSuccessChance,
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1.0 / 7.0), "grow-speed"));
        }
    }
}
