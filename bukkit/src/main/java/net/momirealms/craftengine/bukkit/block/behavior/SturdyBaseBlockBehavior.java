package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SturdyBaseBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<SturdyBaseBlockBehavior> FACTORY = new Factory();
    private final Direction direction;
    private final boolean stackable;
    private final boolean checkFull;
    private final boolean checkRigid;
    private final boolean checkCenter;

    public SturdyBaseBlockBehavior(CustomBlock block, int delay, Direction direction, boolean stackable, boolean checkFull, boolean checkRigid, boolean checkCenter) {
        super(block, delay);
        this.direction = direction;
        this.stackable = stackable;
        this.checkFull = checkFull;
        this.checkRigid = checkRigid;
        this.checkCenter = checkCenter;
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws Exception {
        int x = Vec3iProxy.INSTANCE.getX(blockPos) + this.direction.stepX();
        int y = Vec3iProxy.INSTANCE.getY(blockPos) + this.direction.stepY();
        int z = Vec3iProxy.INSTANCE.getZ(blockPos) + this.direction.stepZ();
        Object targetPos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(world, targetPos);
        if (this.checkFull && (boolean) CoreReflections.method$BlockStateBase$isFaceSturdy.invoke(
                blockState, world, targetPos, DirectionUtils.toNMSDirection(this.direction.opposite()),
                SupportTypeProxy.FULL
        )) {
            return true;
        }
        if (this.checkRigid && BlockProxy.INSTANCE.canSupportRigidBlock(world, targetPos)) {
            return true;
        }
        if (this.checkCenter && BlockProxy.INSTANCE.canSupportCenter(world, targetPos, DirectionUtils.toNMSDirection(this.direction.opposite()))) {
            return true;
        }
        if (!this.stackable) {
            return false;
        }
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        return optionalCustomState.filter(immutableBlockState -> immutableBlockState.owner().value() == super.customBlock).isPresent();
    }

    private static class Factory implements BlockBehaviorFactory<SturdyBaseBlockBehavior> {

        @Override
        public SturdyBaseBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            Direction direction = Direction.valueOf(arguments.getOrDefault("direction", "down").toString().toUpperCase(Locale.ENGLISH));
            boolean stackable = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stackable", false), "stackable");
            List<String> supportTypes = MiscUtils.getAsStringList(arguments.getOrDefault("support-types", List.of("full")));
            return new SturdyBaseBlockBehavior(block, delay, direction, stackable, supportTypes.contains("full"), supportTypes.contains("rigid"), supportTypes.contains("center"));
        }
    }
}
