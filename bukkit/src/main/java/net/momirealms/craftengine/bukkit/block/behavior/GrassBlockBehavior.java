package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.bukkit.world.BukkitBlockInWorld;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Map;

public class GrassBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();

    public GrassBlockBehavior(CustomBlock block) {
        super(block);
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) {
        return FastNMS.INSTANCE.method$GrassBlock$isValidBonemealTarget(args[0], args[1], args[2]);
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception {
        if (!VersionHelper.isOrAbove1_20_2()) return true;
        Object level = args[0];
        Object blockPos = args[2];
        Object blockState = args[3];
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (immutableBlockState == null || immutableBlockState.isEmpty()) {
            return false;
        }
        boolean sendParticles = false;
        Object visualState = immutableBlockState.vanillaBlockState().handle();
        Object visualStateBlock = CoreReflections.method$BlockStateBase$getBlock.invoke(visualState);
        if (CoreReflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
            boolean is = FastNMS.INSTANCE.method$BonemealableBlock$isValidBonemealTarget(visualStateBlock, level, blockPos, visualState);
            if (!is) {
                sendParticles = true;
            }
        } else {
            sendParticles = true;
        }
        if (sendParticles) {
            World world = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
            int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
            int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
            int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
            world.spawnParticle(ParticleUtils.HAPPY_VILLAGER, x + 0.5, y + 1.5, z + 0.5, 20, 2, 0, 2);
        }
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Item<?> item = context.getItem();
        if (item == null || !item.vanillaId().equals(ItemKeys.BONE_MEAL) || context.getPlayer().isAdventureMode())
            return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BukkitBlockInWorld upper = (BukkitBlockInWorld) context.getLevel().getBlockAt(pos.x(), pos.y() + 1, pos.z());
        Block block = upper.block();
        if (!block.isEmpty())
            return InteractionResult.PASS;
        boolean sendSwing = false;
        try {
            Object visualState = state.vanillaBlockState().handle();
            Object visualStateBlock = CoreReflections.method$BlockStateBase$getBlock.invoke(visualState);
            if (CoreReflections.clazz$BonemealableBlock.isInstance(visualStateBlock)) {
                boolean is = FastNMS.INSTANCE.method$BonemealableBlock$isValidBonemealTarget(visualStateBlock, context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()), visualState);
                if (!is) {
                    sendSwing = true;
                }
            } else {
                sendSwing = true;
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to check visual state bone meal state", e);
            return InteractionResult.FAIL;
        }
        if (sendSwing) {
            context.getPlayer().swingHand(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) {
        FastNMS.INSTANCE.method$GrassBlock$performBoneMeal(args[0], args[1], args[2], args[3], thisBlock);
    }

    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            return new GrassBlockBehavior(block);
        }
    }
}
