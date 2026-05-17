package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.BonemealableBlock;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BonemealableBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.ConfiguredFeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class SaplingBlockBehavior extends BukkitBlockBehavior implements BonemealableBlock, RandomTickBlock {
    public static final BlockBehaviorFactory<SaplingBlockBehavior> FACTORY = new Factory();
    public final Key feature;
    public final IntegerProperty stageProperty;
    public final double boneMealSuccessChance;
    public final float growSpeed;

    private SaplingBlockBehavior(BlockDefinition block,
                                 Key feature,
                                 IntegerProperty stageProperty,
                                 double boneMealSuccessChance,
                                 float growSpeed) {
        super(block);
        this.feature = feature;
        this.stageProperty = stageProperty;
        this.boneMealSuccessChance = boneMealSuccessChance;
        this.growSpeed = growSpeed;
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    public Key treeFeature() {
        return feature;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object world = args[1];
        Object blockPos = args[2];
        Object blockState = args[0];
        Object aboveBlockPos = LocationUtils.above(blockPos);
        if (LevelReaderProxy.INSTANCE.getMaxLocalRawBrightness(world, aboveBlockPos) >= 9 && RandomUtils.generateRandomFloat(0, 1) < this.growSpeed) {
            increaseStage(world, blockPos, blockState, args[3]);
        }
    }

    private void increaseStage(Object world, Object blockPos, Object blockState, Object randomSource) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        ImmutableBlockState customState = optionalCustomState.get();
        int currentStage = customState.get(this.stageProperty);
        if (currentStage != this.stageProperty.max) {
            ImmutableBlockState nextStage = customState.cycle(this.stageProperty);
            World bukkitWorld = LevelProxy.INSTANCE.getWorld(world);
            int x = Vec3iProxy.INSTANCE.getX(blockPos);
            int y = Vec3iProxy.INSTANCE.getY(blockPos);
            int z = Vec3iProxy.INSTANCE.getZ(blockPos);
            CraftEngineBlocks.place(new Location(bukkitWorld, x, y, z), nextStage, UpdateFlags.UPDATE_NONE, false);
        } else {
            generateTree(world, blockPos, blockState, randomSource);
        }
    }

    private void generateTree(Object world, Object blockPos, Object blockState, Object randomSource) {
        Object holder = BukkitWorldManager.instance().configuredFeatureHolderById(treeFeature());
        if (holder == null) {
            CraftEngine.instance().logger().warn("Configured feature not found: " + treeFeature());
            return;
        }
        Object chunkGenerator = ServerChunkCacheProxy.INSTANCE.getGenerator(ServerLevelProxy.INSTANCE.getChunkSource(world));
        Object configuredFeature = HolderProxy.INSTANCE.value(holder);
        Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(world, blockPos);
        Object legacyState = FluidStateProxy.INSTANCE.createLegacyBlock(fluidState);
        LevelWriterProxy.INSTANCE.setBlock(world, blockPos, legacyState, UpdateFlags.UPDATE_NONE);
        if (ConfiguredFeatureProxy.INSTANCE.place(configuredFeature, world, chunkGenerator, randomSource, blockPos)) {
            if (BlockGetterProxy.INSTANCE.getBlockState(world, blockPos) == legacyState) {
                ServerLevelProxy.INSTANCE.sendBlockUpdated(world, blockPos, blockState, legacyState, UpdateFlags.UPDATE_CLIENTS);
            }
        } else {
            // failed to place, rollback changes
            LevelWriterProxy.INSTANCE.setBlock(world, blockPos, blockState, UpdateFlags.UPDATE_NONE);
        }
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
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
        Object visualState = customState.visualBlockState().minecraftState();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (BonemealableBlockProxy.CLASS.isInstance(visualStateBlock)) {
            boolean is;
            if (VersionHelper.isOrAbove1_20_2) {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, level, blockPos, visualState);
            } else {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, level, blockPos, visualState, true);
            }
            if (!is) {
                sendParticles = true;
            }
        } else {
            sendParticles = true;
        }
        if (sendParticles) {
            World world = LevelProxy.INSTANCE.getWorld(level);
            int x = Vec3iProxy.INSTANCE.getX(blockPos);
            int y = Vec3iProxy.INSTANCE.getY(blockPos);
            int z = Vec3iProxy.INSTANCE.getZ(blockPos);
            world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 15, 0.25, 0.25, 0.25);
        }
        return success;
    }

    @Override
    public boolean isValidBonemealTarget(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        this.increaseStage(args[0], args[2], args[3], args[1]);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Item item = context.getItem();
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
        Object visualState = state.visualBlockState().minecraftState();
        Object visualStateBlock = BlockStateUtils.getBlockOwner(visualState);
        if (BonemealableBlockProxy.CLASS.isInstance(visualStateBlock)) {
            boolean is;
            if (VersionHelper.isOrAbove1_20_2) {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, world.minecraftWorld(), LocationUtils.toBlockPos(pos), visualState);
            } else {
                is = BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(visualStateBlock, world.minecraftWorld(), LocationUtils.toBlockPos(pos), visualState, true);
            }
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
        private static final String[] FEATURE = new String[]{"feature", "configured_feature", "configured-feature"};
        private static final String[] SUCCESS_CHANCE = new String[]{"bone_meal_success_chance", "bone-meal-success-chance"};
        private static final String[] GROW_SPEED = new String[]{"grow_speed", "grow-speed"};

        @Override
        public SaplingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SaplingBlockBehavior(
                    block,
                    section.getNonNullIdentifier(FEATURE),
                    (IntegerProperty) BlockBehaviorFactory.getProperty(section.path(), block, "stage", Integer.class),
                    section.getDouble(SUCCESS_CHANCE, 0.45d),
                    section.getFloat(GROW_SPEED, 1.0f / 7.0f)
            );
        }
    }
}
