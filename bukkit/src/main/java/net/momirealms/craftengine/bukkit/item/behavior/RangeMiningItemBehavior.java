package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3i;
import net.momirealms.craftengine.core.world.World;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RangeMiningItemBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<RangeMiningItemBehavior> FACTORY = new Factory();
    private final List<Vec3i> miningRange;

    private enum PitchState {
        FLAT, // 平视（挖墙）
        UP,   // 仰视（挖天花板）
        DOWN  // 俯视（挖地板）
    }

    private RangeMiningItemBehavior(List<Vec3i> miningRange) {
        this.miningRange = miningRange;
    }

    @Override
    public void breakBlock(World world, Player player, BlockPos pos) {
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) player;
        if (serverPlayer.isRangeMining()) return;

        BlockStateWrapper blockState = world.getBlockState(pos);
        float destroyProgress = player.getDestroyProgress(blockState.literalObject(), pos);

        // 获取水平朝向 (North, South, East, West)
        Direction facing = player.getDirection();

        // 获取俯仰角并判断状态 (Flat, Up, Down)
        PitchState pitchState = PitchState.FLAT;
        if (player.xRot() < -45) {
            pitchState = PitchState.UP;
        } else if (player.xRot() > 45) {
            pitchState = PitchState.DOWN;
        }

        serverPlayer.setRangeMining(true);
        try {
            for (Vec3i offset : this.miningRange) {
                if (offset.x() == 0 && offset.y() == 0 && offset.z() == 0) continue;
                Vec3i rotatedOffset = rotateOffset(offset, facing, pitchState);

                int targetX = pos.x() + rotatedOffset.x();
                int targetY = pos.y() + rotatedOffset.y();
                int targetZ = pos.z() + rotatedOffset.z();

                BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
                BlockStateWrapper targetBlockState = world.getBlockState(targetPos);

                if (targetBlockState != null && !targetBlockState.isAir()) {
                    float targetProgress = player.getDestroyProgress(targetBlockState.literalObject(), targetPos);
                    // 只有当目标方块比原方块更“脆”或硬度相当时才挖掘
                    if (targetProgress >= destroyProgress) {
                        player.breakBlock(targetX, targetY, targetZ);
                    }
                }
            }
        } finally {
            serverPlayer.setRangeMining(false);
        }
    }

    /**
     * 根据 玩家水平朝向 + 俯仰角状态 计算绝对坐标偏移
     * * @param offset 原始配置偏移 (x=右, y=上, z=深)
     * @param facing 水平朝向 (NSEW)
     * @param pitchState 俯仰状态 (FLAT/UP/DOWN)
     */
    private Vec3i rotateOffset(Vec3i offset, Direction facing, PitchState pitchState) {
        int x = offset.x(); // 相对右
        int y = offset.y(); // 相对上
        int z = offset.z(); // 相对深

        return switch (pitchState) {
            // Case 1: 平视（挖墙）
            // 逻辑：Y轴不变，XZ平面旋转
            case FLAT -> switch (facing) {
                case NORTH -> new Vec3i(x, y, -z);  // 北: z轴向里是-Z
                case SOUTH -> new Vec3i(-x, y, z);  // 南: z轴向里是+Z，x翻转
                case EAST  -> new Vec3i(z, y, x);  // 东: z轴向里是+X
                case WEST  -> new Vec3i(-z, y, -x);  // 西: z轴向里是-X
                default -> offset;
            };

            // Case 2: 仰视（挖天花板）
            // 逻辑：相对深度(z) 变为 世界高度(+Y)。
            // 相对高度(y) 贴合天花板平面。
            // 当你面朝北看天花板时，屏幕上方(相对y)其实是指向南边(背后的天花板)。
            case UP -> switch (facing) {
                case NORTH -> new Vec3i(x, z, y);   // 面向北看天：右是东(+X)，上是南(+Z)
                case SOUTH -> new Vec3i(-x, z, -y); // 面向南看天：右是西(-X)，上是北(-Z)
                case EAST  -> new Vec3i(-y, z, x);  // 面向东看天：右是南(+Z)，上是西(-X)
                case WEST  -> new Vec3i(y, z, -x);  // 面向西看天：右是北(-Z)，上是东(+X)
                default -> new Vec3i(x, z, y);
            };

            // Case 3: 俯视（挖地板）
            // 逻辑：相对深度(z) 变为 世界深度(-Y)。
            // 相对高度(y) 贴合地板平面。
            // 当你面朝北看地板时，屏幕上方(相对y)是指向北边(前方的地板)。
            case DOWN -> switch (facing) {
                case NORTH -> new Vec3i(x, -z, -y); // 面向北看地：右是东(+X)，上是北(-Z)
                case SOUTH -> new Vec3i(-x, -z, y); // 面向南看地：右是西(-X)，上是南(+Z)
                case EAST  -> new Vec3i(y, -z, x);  // 面向东看地：右是南(+Z)，上是东(+X)
                case WEST  -> new Vec3i(-y, -z, -x);// 面向西看地：右是北(-Z)，上是西(-X)
                default -> new Vec3i(x, -z, -y);
            };
        };
    }

    public List<Vec3i> getMiningRange() {
        return this.miningRange;
    }

    private static class Factory implements ItemBehaviorFactory<RangeMiningItemBehavior> {
        @Override
        public RangeMiningItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            List<String> poses = MiscUtils.getAsStringList(arguments.get("range"));
            List<Vec3i> range = poses.stream().map(it -> {
                String[] split = it.split(",", 3);
                if (split.length != 3) return null;
                try {
                    return new Vec3i(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(Objects::nonNull).toList();
            return new RangeMiningItemBehavior(range);
        }
    }
}