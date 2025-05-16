package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FallingBlockRemoveListener implements Listener {

    @EventHandler
    public void onFallingBlockBreak(org.bukkit.event.entity.EntityRemoveEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityRemoveEvent.Cause.DROP && event.getEntity() instanceof FallingBlock fallingBlock) {
            try {
                Object fallingBlockEntity = Reflections.field$CraftEntity$entity.get(fallingBlock);
                boolean cancelDrop = (boolean) Reflections.field$FallingBlockEntity$cancelDrop.get(fallingBlockEntity);
                if (cancelDrop) return;
                Object blockState = Reflections.field$FallingBlockEntity$blockState.get(fallingBlockEntity);
                int stateId = BlockStateUtils.blockStateToId(blockState);
                ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
                if (immutableBlockState == null || immutableBlockState.isEmpty()) return;
                net.momirealms.craftengine.core.world.World world = new BukkitWorld(fallingBlock.getWorld());
                WorldPosition position = new WorldPosition(world, Reflections.field$Entity$xo.getDouble(fallingBlockEntity), Reflections.field$Entity$yo.getDouble(fallingBlockEntity), Reflections.field$Entity$zo.getDouble(fallingBlockEntity));
                ContextHolder.Builder builder = ContextHolder.builder()
                        .withParameter(DirectContextParameters.FALLING_BLOCK, true)
                        .withParameter(DirectContextParameters.POSITION, position);
                for (Item<Object> item : immutableBlockState.getDrops(builder, world, null)) {
                    world.dropItemNaturally(position, item);
                }
                Object entityData = Reflections.field$Entity$entityData.get(fallingBlockEntity);
                boolean isSilent = (boolean) Reflections.method$SynchedEntityData$get.invoke(entityData, Reflections.instance$Entity$DATA_SILENT);
                if (!isSilent) {
                    world.playBlockSound(position, immutableBlockState.sounds().destroySound());
                }
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to handle EntityRemoveEvent", e);
            }
        }
    }
}
