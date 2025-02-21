package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldBlock;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.Map;

public class WaterBucketItemBehavior extends ItemBehavior {
    public static final WaterBucketItemBehavior INSTANCE = new WaterBucketItemBehavior();
    public static final Factory FACTORY = new Factory();

    @SuppressWarnings("unchecked")
    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        BukkitWorldBlock clicked = (BukkitWorldBlock) context.getLevel().getBlockAt(pos);
        Block block = clicked.block();
        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
        if (state == null || state.isEmpty()) return InteractionResult.PASS;
        CustomBlock customBlock = state.owner().value();
        Property<Boolean> waterlogged = (Property<Boolean>) customBlock.getProperty("waterlogged");
        if (waterlogged == null) return InteractionResult.PASS;

        Player player = (Player) context.getPlayer().platformPlayer();
        World world = player.getWorld();
        Location location = new Location(world, pos.x(), pos.y(), pos.z());

        // TODO Refactor all of this because it's playing a trick with the server
        ImmutableBlockState nextState = state.with(waterlogged, true);
        block.setBlockData(BlockStateUtils.createBlockData(nextState.vanillaBlockState().handle()), false);
        // actually we should broadcast this change
        context.getPlayer().sendPacket(BlockStateUtils.createBlockUpdatePacket(pos, state), true);
        BukkitCraftEngine.instance().scheduler().sync().runDelayed(() ->
                CraftEngineBlocks.place(location, nextState, UpdateOption.UPDATE_ALL, false), world, location.getBlockX() >> 4, location.getBlockZ() >> 4);

        return InteractionResult.SUCCESS;
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Pack pack, Path path, Key id, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
