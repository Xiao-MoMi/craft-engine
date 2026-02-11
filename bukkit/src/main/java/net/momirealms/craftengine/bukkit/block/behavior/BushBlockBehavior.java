package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.Tuple;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.*;

public class BushBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<BushBlockBehavior> FACTORY = new Factory();
    protected final List<Object> tagsCanSurviveOn;
    protected final Set<Object> blockStatesCanSurviveOn;
    protected final Set<String> customBlocksCansSurviveOn;
    protected final boolean blacklistMode;
    protected final boolean stackable;
    protected final int maxHeight;

    public BushBlockBehavior(CustomBlock block, int delay, boolean blacklist, boolean stackable, int maxHeight,
                             List<Object> tagsCanSurviveOn, Set<Object> blockStatesCanSurviveOn, Set<String> customBlocksCansSurviveOn) {
        super(block, delay);
        this.blacklistMode = blacklist;
        this.stackable = stackable;
        this.maxHeight = maxHeight;
        this.tagsCanSurviveOn = tagsCanSurviveOn;
        this.blockStatesCanSurviveOn = blockStatesCanSurviveOn;
        this.customBlocksCansSurviveOn = customBlocksCansSurviveOn;
    }

    private static class Factory implements BlockBehaviorFactory<BushBlockBehavior> {

        @Override
        public BushBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Tuple<List<Object>, Set<Object>, Set<String>> tuple = readTagsAndState(arguments, false);
            boolean stackable = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stackable", false), "stackable");
            int maxHeight = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-height", 0), "max-height");
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            boolean blacklistMode = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("blacklist", false), "blacklist");
            return new BushBlockBehavior(block, delay, blacklistMode, stackable, maxHeight,tuple.left(), tuple.mid(), tuple.right());
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
                    mcBlocks.addAll(BlockStateUtils.getPossibleBlockStates(blockType));
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

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws Exception {
        Object belowPos = LocationUtils.below(blockPos);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    protected boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        for (Object tag : this.tagsCanSurviveOn) {
            if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(belowState, tag)) {
                return !this.blacklistMode;
            }
        }
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
        if (optionalCustomState.isEmpty()) {
            if (!this.blockStatesCanSurviveOn.isEmpty() && this.blockStatesCanSurviveOn.contains(belowState)) {
                return !this.blacklistMode;
            }
        } else {
            ImmutableBlockState belowCustomState = optionalCustomState.get();
            if (belowCustomState.owner().value() == super.customBlock) {
                if (!this.stackable || this.maxHeight == 1) return false;
                if (this.maxHeight > 1) {
                    return mayStackOn(world, belowPos);
                }
                return true;
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

    protected boolean mayStackOn(Object world, Object belowPos) {
        int count = 1;
        Object cursorPos = LocationUtils.below(belowPos);

        while (count < this.maxHeight) {
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, cursorPos);
            Optional<ImmutableBlockState> belowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (belowCustomState.isPresent() && belowCustomState.get().owner().value() == super.customBlock) {
                count++;
                cursorPos = LocationUtils.below(cursorPos);
            } else {
                break;
            }
        }
        return count < this.maxHeight;
    }
}
