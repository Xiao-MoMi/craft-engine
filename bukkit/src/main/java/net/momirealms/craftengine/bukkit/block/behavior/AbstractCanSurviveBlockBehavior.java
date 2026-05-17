package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.Bukkit;

import java.util.*;

public abstract class AbstractCanSurviveBlockBehavior extends BukkitBlockBehavior {
    protected final int delay;

    protected AbstractCanSurviveBlockBehavior(BlockDefinition blockDefinition, int delay) {
        super(blockDefinition);
        this.delay = delay;
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        if (this.delay == 0) return;
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        if (!canSurvive(thisBlock, args)) {
            BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
                if (!customState.isEmpty() && customState.owner().value() == this.blockDefinition) {
                    net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
                    WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(LocationUtils.fromBlockPos(blockPos)));
                    world.playBlockSound(position, customState.settings().sounds().breakSound());
                    LevelWriterProxy.INSTANCE.destroyBlock(level, blockPos, true);
                }
            });
        }
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object world = args[1];
        Object pos = args[2];
        return canSurvive(thisBlock, state, world, pos);
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        Object world = args[1];
        Object blockPos = args[2];
        LevelAccessorProxy.INSTANCE.scheduleTick$0(world, blockPos, thisBlock, 2);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object state = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) {
            return state;
        }
        if (this.delay != 0) {
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, blockPos, thisBlock, this.delay);
            return state;
        }
        if (!BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(state, level, blockPos)) {
            LevelAccessorProxy.INSTANCE.levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, optionalCustomState.get().customBlockState().registryId());
            return BlocksProxy.AIR$defaultState;
        }
        return state;
    }

    protected abstract boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos);

    protected static TagsAndState readTagsAndState(ConfigSection section, String prefix) {
        List<Object> mcTags = section.getList(new String[] {prefix + "_block_tags", prefix.replace("_", "-") + "-block-tags"}, v -> BlockTags.getOrCreate(v.getAsIdentifier()));
        Set<Object> blockStates = new HashSet<>();
        List<Key> customBlocks = new ArrayList<>();
        List<String> customStates = new ArrayList<>();
        for (String blockState : section.getStringList(new String[] {prefix + "_blocks", prefix.replace("_", "-") + "-blocks"})) {
            int index = blockState.indexOf('[');
            Key blockType = index != -1 ? Key.of(blockState.substring(0, index)) : Key.of(blockState);
            Object block = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, KeyUtils.toIdentifier(blockType));
            if (block != BlocksProxy.AIR) {
                if (index == -1) {
                    blockStates.addAll(BlockStateUtils.getPossibleBlockStates(blockType));
                } else {
                    blockStates.add(BlockStateUtils.blockDataToBlockState(Bukkit.createBlockData(blockState)));
                }
            } else {
                // custom maybe
                if (index == -1) {
                    customBlocks.add(Key.of(blockState));
                } else {
                    customStates.add(blockState);
                }
            }
        }
        return new TagsAndState(mcTags, LazyReference.lazyReference(() -> {
            for (Key customBlock : customBlocks) {
                BukkitBlockManager.instance().blockById(customBlock).ifPresent(block -> {
                    for (ImmutableBlockState state : block.variantProvider().states()) {
                        blockStates.add(state.customBlockState().minecraftState());
                    }
                });
            }
            for (String customState : customStates) {
                Optional.ofNullable(BlockStateParser.deserialize(customState)).ifPresent(blockState -> {
                    blockStates.add(blockState.customBlockState().minecraftState());
                });
            }
            return blockStates;
        }));
    }

    public record TagsAndState(List<Object> tags, LazyReference<Set<Object>> blockStates) {
    }
}
