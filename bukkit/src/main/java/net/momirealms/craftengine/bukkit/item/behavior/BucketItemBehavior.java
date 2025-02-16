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
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BucketItemBehavior extends ItemBehavior {
    public static final BucketItemBehavior INSTANCE = new BucketItemBehavior();
    public static final Factory FACTORY = new Factory();
    private static final Key ITEM_BUCKET_FILL = Key.of("item.bucket.fill");

    @SuppressWarnings("unchecked")
    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        BukkitWorldBlock clicked = (BukkitWorldBlock) context.getLevel().getBlockAt(context.getClickedPos());
        Block block = clicked.block();
        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
        if (state == null || state.isEmpty()) return InteractionResult.PASS;
        CustomBlock customBlock = state.owner().value();
        Property<Boolean> waterlogged = (Property<Boolean>) customBlock.getProperty("waterlogged");
        if (waterlogged == null) return InteractionResult.PASS;
        boolean waterloggedState = state.get(waterlogged);
        if (!waterloggedState) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        Player player = (Player) context.getPlayer().platformPlayer();
        World world = player.getWorld();
        Location location = new Location(world, pos.x(), pos.y(), pos.z());
        EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;

        CraftEngineBlocks.place(location, state.with(waterlogged, false), UpdateOption.UPDATE_ALL, false);
        if (player.getGameMode() == GameMode.SURVIVAL) {
            // to prevent dupe in moment
            player.getInventory().setItem(slot, new ItemStack(Material.AIR));
            BukkitCraftEngine.instance().scheduler().sync().runDelayed(() ->
                    player.getInventory().setItem(slot, new ItemStack(Material.WATER_BUCKET)), world, location.getBlockX() >> 4, location.getBlockZ() >> 4);
        }
        player.setStatistic(Statistic.USE_ITEM, Material.BUCKET, player.getStatistic(Statistic.USE_ITEM, Material.BUCKET) + 1);
        // client will assume it has sounds
        // context.getPlayer().level().playBlockSound(Vec3d.atCenterOf(context.getClickedPos()), ITEM_BUCKET_FILL, 1, 1);
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Key id, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
