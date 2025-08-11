package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HangingBlockBehavior extends BushBlockBehavior {
    public static final Factory FACTORY = new Factory();

    public HangingBlockBehavior(CustomBlock block, int delay, boolean blacklist, boolean stackable, List<Object> tagsCanSurviveOn, Set<Object> blocksCansSurviveOn, Set<String> customBlocksCansSurviveOn) {
        super(block, delay, blacklist, stackable, tagsCanSurviveOn, blocksCansSurviveOn, customBlocksCansSurviveOn);
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws ReflectiveOperationException {
        Object belowPos = LocationUtils.above(blockPos);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, true);
            boolean stackable = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stackable", false), "stackable");
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            boolean blacklistMode = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blacklist", false), "blacklist");
            return new HangingBlockBehavior(block, delay, blacklistMode, stackable, tuple.left(), tuple.mid(), tuple.right());
        }
    }
}
