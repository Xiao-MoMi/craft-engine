package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class BushBlockBehavior extends BlockBehavior {
    public static final Factory FACTORY = new Factory();
    protected static final Object DIRT_TAG = BlockTags.getOrCreate(Key.of("minecraft", "dirt"));
    protected static final Object FARMLAND = BlockTags.getOrCreate(Key.of("minecraft", "farmland"));
    public static final BushBlockBehavior INSTANCE = new BushBlockBehavior(List.of(DIRT_TAG, FARMLAND));
    protected final List<Object> tagsCanSurviveOn;

    public BushBlockBehavior(List<Object> tagsCanSurviveOn) {
        this.tagsCanSurviveOn = tagsCanSurviveOn;
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
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            level = args[1];
            blockPos = args[3];
        } else {
            level = args[3];
            blockPos = args[4];
        }
        if (!canSurvive(thisBlock, state, level, blockPos)) {
            ImmutableBlockState previousState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(state));
            if (previousState != null && !previousState.isEmpty()) {
                ContextHolder.Builder builder = ContextHolder.builder();
                BlockPos pos = LocationUtils.fromBlockPos(blockPos);
                Vec3d vec3d = Vec3d.atCenterOf(pos);
                net.momirealms.craftengine.core.world.World world = new BukkitWorld((World) Reflections.method$Level$getCraftWorld.invoke(level));
                builder.withParameter(LootParameters.LOCATION, vec3d);
                builder.withParameter(LootParameters.WORLD, world);
                for (Item<Object> item : previousState.getDrops(builder, world)) {
                    world.dropItemNaturally(vec3d, item);
                }
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
            if (arguments.containsKey("tags")) {
                return new BushBlockBehavior(MiscUtils.getAsStringList(arguments.get("tags")).stream().map(it -> BlockTags.getOrCreate(Key.of(it))).toList());
            } else {
                return INSTANCE;
            }
        }
    }

    private boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        int y = Reflections.field$Vec3i$y.getInt(blockPos);
        int x = Reflections.field$Vec3i$x.getInt(blockPos);
        int z = Reflections.field$Vec3i$z.getInt(blockPos);
        Object belowPos = Reflections.constructor$BlockPos.newInstance(x, y - 1, z);
        Object belowState = Reflections.method$BlockGetter$getBlockState.invoke(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    private boolean mayPlaceOn(Object belowState, Object world, Object blockPos) throws ReflectiveOperationException {
        for (Object tag : this.tagsCanSurviveOn) {
            if ((boolean) Reflections.method$BlockStateBase$hasTag.invoke(belowState, tag)) {
                return true;
            }
        }
        return false;
    }
}
