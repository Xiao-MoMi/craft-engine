package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.*;

public class BushBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final Factory FACTORY = new Factory();
    protected final List<Object> tagsCanSurviveOn;
    protected final Set<Object> blockStatesCanSurviveOn;
    protected final Set<String> customBlocksCansSurviveOn;
    protected final boolean blacklistMode;
    protected final boolean stackable;

    public BushBlockBehavior(CustomBlock block, int delay, boolean blacklist, boolean stackable, List<Object> tagsCanSurviveOn, Set<Object> blockStatesCanSurviveOn, Set<String> customBlocksCansSurviveOn) {
        super(block, delay);
        this.blacklistMode = blacklist;
        this.stackable = stackable;
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
        this.customBlocksCansSurviveOn = customBlocksCansSurviveOn;
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, false);
            boolean stackable = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stackable", false), "stackable");
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            boolean blacklistMode = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blacklist", false), "blacklist");
            return new BushBlockBehavior(block, delay, blacklistMode, stackable, tuple.left(), tuple.mid(), tuple.right());
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

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws Exception {
        Object belowPos = LocationUtils.below(blockPos);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    protected boolean mayPlaceOn(Object blockState, Object world, Object belowPos) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (FastNMS.INSTANCE.method$BlockStateBase$is(blockState, tag)) {
                return !this.blacklistMode;
            }
        }
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            if (!this.blockStatesCanSurviveOn.isEmpty() && this.blockStatesCanSurviveOn.contains(blockState)) {
                return !this.blacklistMode;
            }
        } else {
            ImmutableBlockState belowCustomState = optionalCustomState.get();
            if (belowCustomState.owner().value() == super.customBlock) {
                return this.stackable;
            }
            if (this.customBlocksCansSurviveOn.contains(belowCustomState.owner().value().id().toString())) {
                return !this.blacklistMode;
            }
            if (this.customBlocksCansSurviveOn.contains(belowCustomState.toString())) {
                return !this.blacklistMode;
            }
        }
        return this.blacklistMode;
    }
}
