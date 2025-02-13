package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FallingBlockRemoveListener implements Listener {

    /*
     * This is not an event that would be removed
     * Paper mistakenly marked it as deprecated in 1.20.4
     */
    @SuppressWarnings("removal")
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
                ContextHolder.Builder builder = ContextHolder.builder();
                builder.withParameter(LootParameters.FALLING_BLOCK, true);
                double x = Reflections.field$Entity$xo.getDouble(fallingBlockEntity);
                double y = Reflections.field$Entity$yo.getDouble(fallingBlockEntity);
                double z = Reflections.field$Entity$zo.getDouble(fallingBlockEntity);
                Vec3d vec3d = new Vec3d(x, y, z);
                net.momirealms.craftengine.core.world.World world = new BukkitWorld(fallingBlock.getWorld());
                builder.withParameter(LootParameters.LOCATION, vec3d);
                builder.withParameter(LootParameters.WORLD, world);
                for (Item<Object> item : immutableBlockState.getDrops(builder, world)) {
                    world.dropItemNaturally(vec3d, item);
                }
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to handle EntityRemoveEvent", e);
            }
        }
    }
}
