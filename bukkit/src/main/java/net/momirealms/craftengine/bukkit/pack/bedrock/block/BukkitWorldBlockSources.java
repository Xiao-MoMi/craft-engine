package net.momirealms.craftengine.bukkit.pack.bedrock.block;

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
        List<BlockDefinition> ordered = new ArrayList<>(definitions);
        ordered.sort(Comparator.comparing(block -> block.id().asString()));
        List<WorldBlockSource> result = new ArrayList<>();
        Object context = CollisionContextProxy.INSTANCE.empty();
        for (BlockDefinition block : ordered) {
            int index = 0;
            for (ImmutableBlockState state : block.variantProvider().states()) {
                Object nms = state.customBlockState().minecraftState();
                Object collision = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCollisionShape(nms,
                        EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO, context);
                List<Object> boxes = VoxelShapeProxy.INSTANCE.toAabbs(collision);
                List<AABB> collisionBoxes = new ArrayList<>(boxes.size());
                for (Object box : boxes) {
                    collisionBoxes.add(CollisionUtils.fromMinecraftAabb(box));
                }
                Object selection = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getShape(nms,
                        EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO, context);
                AABB selectionBox = VoxelShapeProxy.INSTANCE.isEmpty(selection) ? null : CollisionUtils.fromMinecraftAabb(VoxelShapeProxy.INSTANCE.bounds(selection));
                int lightDampening = VersionHelper.isOrAbove1_21_2
                        ? BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightDampening$1(nms)
                        : BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getLightBlock(nms, EmptyBlockGetterProxy.GETTER_INSTANCE, BlockPosProxy.ZERO);
                result.add(new WorldBlockSource(block.id(), index++, state.customBlockState().registryId(),
                        new BukkitVanillaBlockStateWrapper(nms, state.customBlockState().registryId()).getAsString(), state.visualBlockState(), state.settings().hardness(),
                        state.settings().luminance(), lightDampening, state.settings().friction(), collisionBoxes,
                        selectionBox, state.hasConstantBlockEntityRenderer()));
            }
        }
        return result;
    }
}
