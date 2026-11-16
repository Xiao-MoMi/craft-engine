package net.momirealms.craftengine.bukkit.pack.bedrock.block;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.block.BukkitVanillaBlockStateWrapper;
import net.momirealms.craftengine.bukkit.util.CollisionUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.pack.bedrock.block.WorldBlockSource;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.EmptyBlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.VoxelShapeProxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class BukkitWorldBlockSources {

    private BukkitWorldBlockSources() {
    }

    public static List<WorldBlockSource> collect(Collection<BlockDefinition> definitions) {
        List<BlockDefinition> orderedDefinitions = new ObjectArrayList<>(definitions);
        orderedDefinitions.sort(Comparator.comparing(block -> block.id().asString()));
        List<WorldBlockSource> sources = new ObjectArrayList<>();
        Object collisionContext = CollisionContextProxy.INSTANCE.empty();
        for (BlockDefinition block : orderedDefinitions) {
            int stateIndex = 0;
            for (ImmutableBlockState state : block.variantProvider().states()) {
                sources.add(createSource(block, state, stateIndex++, collisionContext));
            }
        }
        return sources;
    }

    private static WorldBlockSource createSource(BlockDefinition block, ImmutableBlockState state, int stateIndex, Object collisionContext) {
        Object minecraftState = state.customBlockState().minecraftState();
        Object collisionShape = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCollisionShape(minecraftState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO, collisionContext);
        List<Object> minecraftCollisionBoxes = VoxelShapeProxy.INSTANCE.toAabbs(collisionShape);
        List<AABB> collisionBoxes = new ArrayList<>(minecraftCollisionBoxes.size());
        for (Object box : minecraftCollisionBoxes) {
            collisionBoxes.add(CollisionUtils.fromMinecraftAabb(box));
        }

        Object selectionShape = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getShape(
                minecraftState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO, collisionContext);
        AABB selectionBox = VoxelShapeProxy.INSTANCE.isEmpty(selectionShape)
                ? null
                : CollisionUtils.fromMinecraftAabb(VoxelShapeProxy.INSTANCE.bounds(selectionShape));
        int lightDampening = VersionHelper.isOrAbove1_21_2
                ? BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightDampening$1(minecraftState)
                : BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightBlock(minecraftState, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO);

        int serverStateId = state.customBlockState().registryId();
        String serverState = new BukkitVanillaBlockStateWrapper(minecraftState, serverStateId).getAsString();
        return new WorldBlockSource(
                block.id(),
                stateIndex,
                serverStateId,
                serverState,
                state.visualBlockState(),
                state.settings().hardness(),
                state.settings().luminance(),
                lightDampening,
                state.settings().friction(),
                collisionBoxes,
                selectionBox
        );
    }
}
