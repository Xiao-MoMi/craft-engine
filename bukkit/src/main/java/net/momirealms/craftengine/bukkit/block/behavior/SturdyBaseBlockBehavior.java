package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

import java.util.List;

public final class SturdyBaseBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<SturdyBaseBlockBehavior> FACTORY = new Factory();
    public final Direction direction;
    public final boolean stackable;
    public final boolean checkFull;
    public final boolean checkRigid;
    public final boolean checkCenter;
    public final int maxHeight;

    private SturdyBaseBlockBehavior(BlockDefinition block,
                                    int delay,
                                    Direction direction,
                                    boolean stackable,
                                    boolean checkFull,
                                    boolean checkRigid,
                                    boolean checkCenter,
                                    int maxHeight
    ) {
        super(block, delay);
        this.direction = direction;
        this.stackable = stackable;
        this.checkFull = checkFull;
        this.checkRigid = checkRigid;
        this.checkCenter = checkCenter;
        this.maxHeight = maxHeight;
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        int x = Vec3iProxy.INSTANCE.getX(blockPos) + this.direction.stepX();
        int y = Vec3iProxy.INSTANCE.getY(blockPos) + this.direction.stepY();
        int z = Vec3iProxy.INSTANCE.getZ(blockPos) + this.direction.stepZ();
        Object targetPos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, targetPos);

        // Full
        if (this.checkFull && BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(
                blockState, level, targetPos, DirectionUtils.toNMSDirection(this.direction.opposite()), SupportTypeProxy.FULL
        )) {
            return true;
        }
        // Rigid
        if (this.checkRigid && BlockProxy.INSTANCE.canSupportRigidBlock(level, targetPos)) {
            return true;
        }
        // Center
        if (this.checkCenter && BlockProxy.INSTANCE.canSupportCenter(level, targetPos, DirectionUtils.toNMSDirection(this.direction.opposite()))) {
            return true;
        }
        // 如果不允许堆叠, 则直接损坏.
        if (!this.stackable || this.maxHeight <= 1) {
            return false;
        }

        // 检查依靠的方块是否也是相同方块.
        boolean isSameCustomBlock = BlockStateUtils.getOptionalCustomBlockState(blockState)
                .map(immutableBlockState -> immutableBlockState.owner().value() == super.blockDefinition)
                .orElse(false);

        // 如果依靠的方块既不能提供基础支撑，也不是相同方块，则无法存活.
        if (!isSameCustomBlock) {
            return false;
        }

        // 检查剩下的方块, 当前有没有超高.
        for (int i = 1; i < this.maxHeight; i++) {
            x += this.direction.stepX();
            y += this.direction.stepY();
            z += this.direction.stepZ();
            targetPos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
            blockState = BlockGetterProxy.INSTANCE.getBlockState(level, targetPos);

            boolean isCurrentSame = BlockStateUtils.getOptionalCustomBlockState(blockState)
                    .map(immutableBlockState -> immutableBlockState.owner().value() == super.blockDefinition)
                    .orElse(false);

            // 如果方向找到了非同类的方块, 则存活.
            if (!isCurrentSame) {
                return true;
            }
        }

        // 超过了 maxHeight 依然没有找到非同类方块, 说明超高了, 损坏.
        return false;
    }

    private static class Factory implements BlockBehaviorFactory<SturdyBaseBlockBehavior> {
        private static final String[] SUPPORT_TYPES = new String[] {"support_types", "support-types"};
        private static final String[] MAX_HEIGHT = new String[] {"max_height", "max-height"};

        @Override
        public SturdyBaseBlockBehavior create(BlockDefinition block, ConfigSection section) {
            List<String> supportTypes = section.getStringList(SUPPORT_TYPES, List.of("full"));
            return new SturdyBaseBlockBehavior(
                    block,
                    section.getInt("delay", 0),
                    section.getEnum("direction", Direction.class, Direction.DOWN),
                    section.getBoolean("stackable"),
                    supportTypes.contains("full"),
                    supportTypes.contains("rigid"),
                    supportTypes.contains("center"),
                    section.getInt(MAX_HEIGHT)
            );
        }
    }
}
