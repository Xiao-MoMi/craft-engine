package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.FeatureUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public class SaplingBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<SaplingBlockBehavior> FACTORY = new Factory();
    private final Key feature;
    private final IntegerProperty stageProperty;
    private final double boneMealSuccessChance;
    private final float growSpeed;

    public SaplingBlockBehavior(CustomBlock block, Key feature, IntegerProperty stageProperty, double boneMealSuccessChance, float growSpeed) {
        super(block);
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
        if ((int) CoreReflections.method$LevelReader$getMaxLocalRawBrightness.invoke(world, aboveBlockPos) >= 9 && RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
            increaseStage(world, blockPos, blockState, args[3]);
        }
    }

    private void increaseStage(Object world, Object blockPos, Object blockState, Object randomSource) throws Exception {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        ImmutableBlockState customState = optionalCustomState.get();
        int currentStage = customState.get(this.stageProperty);
        if (currentStage != this.stageProperty.max) {
            ImmutableBlockState nextStage = customState.cycle(this.stageProperty);
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
        Object holder = BukkitWorldManager.instance().configuredFeatureById(treeFeature());
        if (holder == null) {
            Object registry = FastNMS.INSTANCE.method$RegistryAccess$lookupOrThrow(FastNMS.INSTANCE.registryAccess(), MRegistries.CONFIGURED_FEATURE);
            if (registry == null) return;
            Optional<Object> optionalHolder = FastNMS.INSTANCE.method$Registry$getHolderByResourceKey(registry, FeatureUtils.createConfiguredFeatureKey(treeFeature()));
            if (optionalHolder.isEmpty()) {
                CraftEngine.instance().logger().warn("Configured feature not found: " + treeFeature());
                return;
            }
            holder = optionalHolder.get();
        }
        Object chunkGenerator = CoreReflections.method$ServerChunkCache$getGenerator.invoke(FastNMS.INSTANCE.method$ServerLevel$getChunkSource(world));
        Object configuredFeature = FastNMS.INSTANCE.method$Holder$value(holder);
        Object fluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(world, blockPos);
        Object legacyState = CoreReflections.method$FluidState$createLegacyBlock.invoke(fluidState);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(world, blockPos, legacyState, UpdateOption.UPDATE_NONE.flags());
        if ((boolean) CoreReflections.method$ConfiguredFeature$place.invoke(configuredFeature, world, chunkGenerator, randomSource, blockPos)) {
            if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, blockPos) == legacyState) {
                CoreReflections.method$ServerLevel$sendBlockUpdated.invoke(world, blockPos, blockState, legacyState, 2);
            }
        } else {
            // failed to place, rollback changes
            FastNMS.INSTANCE.method$LevelWriter$setBlock(world, blockPos, blockState, UpdateOption.UPDATE_NONE.flags());
        }
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        boolean success = RandomUtils.generateRandomDouble(0d, 1d) < this.boneMealSuccessChance;
        Object level = args[0];
        Object blockPos = args[2];
        Object blockState = args[3];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            return false;
        }
        ImmutableBlockState customState = optionalCustomState.get();
        boolean sendParticles = false;
        Object visualState = customState.visualBlockState().literalObject();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (CoreReflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
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

    @SuppressWarnings("DuplicatedCode")
    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Item<?> item = context.getItem();
        Player player = context.getPlayer();
        if (ItemUtils.isEmpty(item) || !item.vanillaId().equals(ItemKeys.BONE_MEAL) || player == null || player.isAdventureMode())
            return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        net.momirealms.craftengine.core.world.World world = context.getLevel();
        Location location = new Location((World) world.platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.INTERACT, location)) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        boolean sendSwing = false;
        Object visualState = state.visualBlockState().literalObject();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (CoreReflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
            boolean is = FastNMS.INSTANCE.method$BonemealableBlock$isValidBonemealTarget(visualStateBlock, world.serverWorld(), LocationUtils.toBlockPos(pos), visualState);
            if (!is) {
                sendSwing = true;
            }
        } else {
            sendSwing = true;
        }
        if (sendSwing) {
            player.swingHand(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    private static class Factory implements BlockBehaviorFactory<SaplingBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public SaplingBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String feature = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "feature", "configured-feature"), "warning.config.block.behavior.sapling.missing_feature");
            Property<Integer> stageProperty = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("stage"), "warning.config.block.behavior.sapling.missing_stage");
            double boneMealSuccessChance = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("bone-meal-success-chance", 0.45), "bone-meal-success-chance");
            return new SaplingBlockBehavior(block, Key.of(feature), (IntegerProperty) stageProperty, boneMealSuccessChance,
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("grow-speed", 1.0 / 7.0), "grow-speed"));
        }
    }
}
