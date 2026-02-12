package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.FeatureUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class GrassBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<GrassBlockBehavior> FACTORY = new Factory();
    private final Key feature;

    public GrassBlockBehavior(CustomBlock block, Key feature) {
        super(block);
        this.feature = feature;
    }

    public Key boneMealFeature() {
        return feature;
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        Object above = LocationUtils.above(args[1]);
        Object aboveState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(args[0], above);
        return FastNMS.INSTANCE.method$BlockStateBase$isAir(aboveState);
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) {
        if (!VersionHelper.isOrAbove1_20_2()) return true;
        Object level = args[0];
        Object blockPos = args[2];
        Object blockState = args[3];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            return false;
        }
        boolean sendParticles = false;
        ImmutableBlockState customState = optionalCustomState.get();
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
            world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 1.5, z + 0.5, 20, 2, 0, 2);
        }
        return true;
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
        BukkitExistingBlock upper = (BukkitExistingBlock) world.getBlock(pos.x(), pos.y() + 1, pos.z());
        Block block = upper.block();
        if (!block.isEmpty())
            return InteractionResult.PASS;
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

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
        Object registry = FastNMS.INSTANCE.method$RegistryAccess$lookupOrThrow(FastNMS.INSTANCE.registryAccess(), MRegistries.PLACED_FEATURE);
        if (registry == null) return;
        Optional<Object> holder = FastNMS.INSTANCE.method$Registry$getHolderByResourceKey(registry, FeatureUtils.createPlacedFeatureKey(boneMealFeature()));
        if (holder.isEmpty()) {
            CraftEngine.instance().logger().warn("Placed feature not found: " + boneMealFeature());
            return;
        }
        BlockPos grassPos = LocationUtils.fromBlockPos(args[2]);
        Object world = args[0];
        Object random = args[1];
        BlockPos topPos = grassPos.above();
        out:
        for (int i = 0; i < 128; i++) {
            BlockPos currentPos = topPos;
            for (int j = 0; j < i / 16; ++j) {
                currentPos = currentPos.offset(
                        RandomUtils.generateRandomInt(-1, 2), RandomUtils.generateRandomInt(-1, 2) * RandomUtils.generateRandomInt(0, 3) / 2, RandomUtils.generateRandomInt(-1, 2)
                );
                BlockPos belowPos = currentPos.relative(Direction.DOWN);
                Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, LocationUtils.toBlockPos(belowPos));
                Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
                if (optionalCustomState.isEmpty()) {
                    continue out;
                }
                if (optionalCustomState.get().owner().value() != super.customBlock) {
                    continue out;
                }
                Object nmsCurrentPos = LocationUtils.toBlockPos(currentPos);
                Object currentState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, nmsCurrentPos);
                if (FastNMS.INSTANCE.method$BlockStateBase$isCollisionShapeFullBlock(currentState, world, nmsCurrentPos)) {
                    continue out;
                }
                if (BlockStateUtils.getBlockOwner(currentState) == MBlocks.SHORT_GRASS && RandomUtils.generateRandomInt(0, 10) == 0) {
                    CoreReflections.method$BonemealableBlock$performBonemeal.invoke(MBlocks.SHORT_GRASS, world, random, nmsCurrentPos, currentState);
                }
                if (FastNMS.INSTANCE.method$BlockStateBase$isAir(currentState)) {
                    Object chunkGenerator = CoreReflections.method$ServerChunkCache$getGenerator.invoke(FastNMS.INSTANCE.method$ServerLevel$getChunkSource(world));
                    Object placedFeature = FastNMS.INSTANCE.method$Holder$value(holder.get());
                    CoreReflections.method$PlacedFeature$place.invoke(placedFeature, world, chunkGenerator, random, nmsCurrentPos);
                }
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<GrassBlockBehavior> {
        @Override
        public GrassBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String feature = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "feature", "placed-feature"), "warning.config.block.behavior.grass.missing_feature");
            return new GrassBlockBehavior(block, Key.of(feature));
        }
    }
}
