package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.*;
import java.util.concurrent.Callable;

public class BushBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    protected final List<Object> tagsCanSurviveOn;
    protected final Set<Object> blocksCansSurviveOn;
    protected final Set<String> customBlocksCansSurviveOn;
    protected final boolean any;

    public BushBlockBehavior(CustomBlock block, List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn) {
        super(block);
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.blocksCansSurviveOn = blocksCansSurviveOn;
        this.customBlocksCansSurviveOn = customBlocksCansSurviveOn;
        this.any = this.tagsCanSurviveOn.isEmpty() && this.blocksCansSurviveOn.isEmpty() && this.customBlocksCansSurviveOn.isEmpty();
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world = args[1];
        Object blockPos = args[2];
        Reflections.method$LevelAccessor$scheduleTick.invoke(world, blockPos, thisBlock, 2);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level;
        Object blockPos;
        Object state = args[0];
        if (VersionHelper.isOrAbove1_21_2()) {
            level = args[1];
            blockPos = args[3];
        } else {
            level = args[3];
            blockPos = args[4];
        }
        if (!canSurvive(thisBlock, state, level, blockPos)) {
            int stateId = BlockStateUtils.blockStateToId(state);
            ImmutableBlockState previousState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
            if (previousState != null && !previousState.isEmpty()) {
                BlockPos pos = LocationUtils.fromBlockPos(blockPos);
                net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
                WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
                ContextHolder.Builder builder = ContextHolder.builder()
                        .withParameter(DirectContextParameters.POSITION, position);
                for (Item<Object> item : previousState.getDrops(builder, world, null)) {
                    world.dropItemNaturally(position, item);
                }
                world.playBlockSound(position, previousState.sounds().breakSound());
                FastNMS.INSTANCE.method$Level$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, stateId);
            }
            return Reflections.method$Block$defaultBlockState.invoke(Reflections.instance$Blocks$AIR);
        }
        return super.updateShape(thisBlock, args, superMethod);
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object world = args[1];
        Object pos = args[2];
        return canSurvive(thisBlock, state, world, pos);
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, false);
            return new BushBlockBehavior(block, tuple.left(), tuple.mid(), tuple.right());
        }
    }

    public static Tuple<List<Object>, Set<Object>, Set<String>> readTagsAndState(Map<String, Object> arguments, boolean aboveOrBelow) {
        List<Object> mcTags = new ArrayList<>();
        for (String tag : MiscUtils.getAsStringList(arguments.getOrDefault((aboveOrBelow ? "above" : "bottom") + "-block-tags", List.of()))) {
            mcTags.add(BlockTags.getOrCreate(Key.of(tag)));
        }
        Set<Object> mcBlocks = new HashSet<>();
        Set<String> customBlocks = new HashSet<>();
        for (String blockStateStr : MiscUtils.getAsStringList(arguments.getOrDefault((aboveOrBelow ? "above" : "bottom") + "-blocks", List.of()))) {
            int index = blockStateStr.indexOf('[');
            Key blockType = index != -1 ? Key.from(blockStateStr.substring(0, index)) : Key.from(blockStateStr);
            Material material = Registry.MATERIAL.get(new NamespacedKey(blockType.namespace(), blockType.value()));
            if (material != null) {
                if (index == -1) {
                    // vanilla
                    mcBlocks.addAll(BlockStateUtils.getAllVanillaBlockStates(blockType));
                } else {
                    mcBlocks.add(BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(blockStateStr)));
                }
            } else {
                // custom maybe
                customBlocks.add(blockStateStr);
            }
        }
        return new Tuple<>(mcTags, mcBlocks, customBlocks);
    }

    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y - 1, z);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    protected boolean mayPlaceOn(Object belowState, Object world, Object belowPos) throws ReflectiveOperationException {
        if (this.any) return true;
        for (Object tag : this.tagsCanSurviveOn) {
            if ((boolean) Reflections.method$BlockStateBase$hasTag.invoke(belowState, tag)) {
                return true;
            }
        }
        int id = BlockStateUtils.blockStateToId(belowState);
        if (BlockStateUtils.isVanillaBlock(id)) {
            if (!this.blocksCansSurviveOn.isEmpty() && this.blocksCansSurviveOn.contains(belowState)) {
                return true;
            }
        } else {
            ImmutableBlockState previousState = BukkitBlockManager.instance().getImmutableBlockState(id);
            if (previousState != null && !previousState.isEmpty()) {
                if (this.customBlocksCansSurviveOn.contains(previousState.owner().value().id().toString())) {
                    return true;
                }
                if (this.customBlocksCansSurviveOn.contains(previousState.toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
