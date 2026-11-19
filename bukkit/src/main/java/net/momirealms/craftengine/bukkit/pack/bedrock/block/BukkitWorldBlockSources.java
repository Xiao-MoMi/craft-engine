package net.momirealms.craftengine.bukkit.pack.bedrock.block;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.block.BukkitVanillaBlockStateWrapper;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.CollisionUtils;
import net.momirealms.craftengine.bukkit.util.SoundUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.pack.bedrock.block.JavaBlockModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockPlan;
import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockProperties;
import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockSource;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.EmptyBlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.VoxelShapeProxy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BukkitWorldBlockSources {

    private BukkitWorldBlockSources() {
    }

    public static WorldBlockPlan plan(BukkitCraftEngine plugin, JavaBlockModelResolver models) {
        Object collisionContext = CollisionContextProxy.INSTANCE.empty();
        List<WorldBlockSource> customBlocks = collect(plugin.blockManager().loadedBlocks().values(), collisionContext);
        Map<Object, BlockSounds> sounds = new IdentityHashMap<>();
        return WorldBlockPlan.create(plugin, models, customBlocks, state -> vanillaProperties(state, collisionContext, sounds));
    }

    private static List<WorldBlockSource> collect(Collection<BlockDefinition> definitions, Object collisionContext) {
        List<BlockDefinition> orderedDefinitions = new ObjectArrayList<>(definitions);
        orderedDefinitions.sort(Comparator.comparing(block -> block.id().asString()));
        List<WorldBlockSource> sources = new ObjectArrayList<>();
        for (BlockDefinition block : orderedDefinitions) {
            int stateIndex = 0;
            for (ImmutableBlockState state : block.variantProvider().states()) {
                sources.add(createSource(block, state, stateIndex++, collisionContext));
            }
        }
        return sources;
    }

    private static WorldBlockPlan.VanillaProperties vanillaProperties(BlockStateWrapper state, Object collisionContext, Map<Object, BlockSounds> sounds) {
        Object minecraftState = state.minecraftState();
        BlockBehaviourProxy.BlockStateBaseProxy proxy = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE;
        if (proxy.hasBlockEntity(minecraftState)) {
            throw new UnsupportedOperationException("the Bedrock replacement cannot preserve the vanilla block entity");
        }
        if (FluidStateProxy.INSTANCE.getType(proxy.getFluidState(minecraftState)) != FluidsProxy.EMPTY) {
            throw new UnsupportedOperationException("the Bedrock replacement cannot preserve the vanilla fluid state");
        }
        WorldBlockProperties properties = new WorldBlockProperties(
                proxy.getDestroySpeed(minecraftState),
                proxy.getLightEmission(minecraftState),
                lightDampening(minecraftState),
                BlockBehaviourProxy.INSTANCE.getFriction(proxy.getBlock(minecraftState)),
                collisionBoxes(minecraftState, collisionContext),
                selectionBox(minecraftState, collisionContext)
        );
        BlockSounds blockSounds = sounds.computeIfAbsent(proxy.getSoundType(minecraftState), SoundUtils::fromNMSSoundType);
        return new WorldBlockPlan.VanillaProperties(properties, blockSounds);
    }

    private static WorldBlockSource createSource(BlockDefinition block, ImmutableBlockState state, int stateIndex, Object collisionContext) {
        Object minecraftState = state.customBlockState().minecraftState();
        int serverStateId = state.customBlockState().registryId();
        String serverState = new BukkitVanillaBlockStateWrapper(minecraftState, serverStateId).getAsString();
        return new WorldBlockSource(
                block.id(),
                stateIndex,
                serverStateId,
                serverState,
                state.visualBlockState(),
                new WorldBlockProperties(
                        state.settings().hardness(),
                        state.settings().luminance(),
                        lightDampening(minecraftState),
                        state.settings().friction(),
                        collisionBoxes(minecraftState, collisionContext),
                        selectionBox(minecraftState, collisionContext)
                ),
                state.settings().sounds()
        );
    }

    private static List<AABB> collisionBoxes(Object minecraftState, Object collisionContext) {
        Object collisionShape = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCollisionShape(minecraftState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO, collisionContext);
        List<Object> minecraftCollisionBoxes = VoxelShapeProxy.INSTANCE.toAabbs(collisionShape);
        List<AABB> collisionBoxes = new ArrayList<>(minecraftCollisionBoxes.size());
        for (Object box : minecraftCollisionBoxes) {
            collisionBoxes.add(CollisionUtils.fromMinecraftAabb(box));
        }

        return collisionBoxes;
    }

    private static @Nullable AABB selectionBox(Object minecraftState, Object collisionContext) {
        Object selectionShape = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getShape(
                minecraftState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO, collisionContext);
        return VoxelShapeProxy.INSTANCE.isEmpty(selectionShape)
                ? null
                : CollisionUtils.fromMinecraftAabb(VoxelShapeProxy.INSTANCE.bounds(selectionShape));
    }

    private static int lightDampening(Object minecraftState) {
        return VersionHelper.isOrAbove1_21_2
                ? BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightDampening$1(minecraftState)
                : BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightBlock(minecraftState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO);
    }
}
