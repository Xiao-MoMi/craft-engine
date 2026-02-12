package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.GameEvent;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.Vector;

import java.nio.file.Path;
import java.util.Map;

public final class CompostableItemBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<CompostableItemBehavior> FACTORY = new Factory();
    private final double chance;

    private CompostableItemBehavior(double chance) {
        this.chance = chance;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        BukkitExistingBlock block = (BukkitExistingBlock) context.getLevel().getBlock(context.getClickedPos());
        BlockData blockData = block.block().getBlockData();
        Object blockOwner = BlockStateUtils.getBlockOwner(BlockStateUtils.blockDataToBlockState(blockData));
        if (blockOwner != MBlocks.COMPOSTER) return InteractionResult.PASS;
        if (!(blockData instanceof Levelled levelled)) {
            return InteractionResult.PASS;
        }

        int maxLevel = levelled.getMaximumLevel();
        int currentLevel = levelled.getLevel();
        if (currentLevel >= maxLevel) return InteractionResult.PASS;
        boolean willRaise = (currentLevel == 0) && (this.chance > 0) || (RandomUtils.generateRandomDouble(0, 1) < this.chance);

        Player player = context.getPlayer();
        if (willRaise) {
            levelled.setLevel(currentLevel + 1);
            if (player != null) {
                EntityChangeBlockEvent event = new EntityChangeBlockEvent((Entity) player.platformPlayer(), block.block(), levelled);
                if (EventUtils.fireAndCheckCancel(event)) {
                    return InteractionResult.FAIL;
                }
            }
            block.block().setBlockData(levelled);
        }

        context.getLevel().levelEvent(WorldEvents.COMPOSTER_COMPOSTS, context.getClickedPos(), willRaise ? 1 : 0);
        ((World) context.getLevel().platformWorld()).sendGameEvent(player != null ? (Entity) player.platformPlayer() : null, GameEvent.BLOCK_CHANGE, new Vector(block.x() + 0.5, block.y() + 0.5, block.z() + 0.5));
        if (currentLevel + 1 == 7) {
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()), blockOwner, 20);
        }
        if (player != null) {
            if (!player.canInstabuild()) {
                context.getItem().shrink(1);
            }
            player.swingHand(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    private static class Factory implements ItemBehaviorFactory<CompostableItemBehavior> {
        @Override
        public CompostableItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            double chance = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("chance", 0.55), "chance");
            return new CompostableItemBehavior(chance);
        }
    }
}
