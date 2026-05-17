package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.MutableVec3d;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.collision.AABB;

public final class EntityCulling {
    private static final int MASK_X_POS = (1 << 0) | (1 << 2) | (1 << 4) | (1 << 6) | (1 << 10);
    private static final int MASK_X_NEG = (1 << 1) | (1 << 3) | (1 << 5) | (1 << 7) | (1 << 11);
    private static final int MASK_Y_POS = (1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 12);
    private static final int MASK_Y_NEG = (1 << 4) | (1 << 5) | (1 << 6) | (1 << 7) | (1 << 13);
    private static final int MASK_Z_POS = (1 << 0) | (1 << 1) | (1 << 4) | (1 << 5) | (1 << 8);
    private static final int MASK_Z_NEG = (1 << 2) | (1 << 3) | (1 << 6) | (1 << 7) | (1 << 9);
    public static final int MAX_SAMPLES = 14;
    private final Player player;
    private final MutableVec3d[] targetPoints = new MutableVec3d[MAX_SAMPLES];
    private final MutableVec3d direction = new MutableVec3d(0,0,0);
    private int lastVisitChunkX = Integer.MAX_VALUE;
    private int lastVisitChunkZ = Integer.MAX_VALUE;
    private ClientChunk lastVisitChunk = null;
    private int currentTokens = Config.entityCullingRateLimitingBucketSize();
    private double distanceScale = 1d;
    public int[] lastHitBlock = new int[3];
    public boolean canCheckLastHitBlock = false;

    public EntityCulling(Player player) {
        this.player = player;
        for (int i = 0; i < MAX_SAMPLES; i++) {
            this.targetPoints[i] = new MutableVec3d(0,0,0);
        }
    }

    public void setDistanceScale(double distanceScale) {
        this.distanceScale = distanceScale;
    }

    public double distanceScale() {
        return distanceScale;
    }

    public void restoreTokenOnTick() {
        this.currentTokens = Math.min(Config.entityCullingRateLimitingBucketSize(), this.currentTokens + Config.entityCullingRateLimitingRestorePerTick());
    }

    public boolean takeToken() {
        if (this.currentTokens > 0) {
            this.currentTokens--;
            return true;
        }
        return false;
    }

    public boolean isVisible(CullingData cullable, Vec3d cameraPos, boolean rayTracing) {
        // 情空标志位
        AABB aabb = cullable.aabb;
        double aabbExpansion = cullable.aabbExpansion;

        double minX = aabb.minX - aabbExpansion;
        double minY = aabb.minY - aabbExpansion;
        double minZ = aabb.minZ - aabbExpansion;
        double maxX = aabb.maxX + aabbExpansion;
        double maxY = aabb.maxY + aabbExpansion;
        double maxZ = aabb.maxZ + aabbExpansion;

        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        Relative relX = Relative.from(minX, maxX, cameraX);
        Relative relY = Relative.from(minY, maxY, cameraY);
        Relative relZ = Relative.from(minZ, maxZ, cameraZ);

        // 相机位于实体内部
        if (relX == Relative.INSIDE && relY == Relative.INSIDE && relZ == Relative.INSIDE) {
            return true;
        }

        // 如果设置了最大距离
        double maxDistance = cullable.maxDistance * this.distanceScale;
        if (maxDistance > 0) {
            // 计算AABB到相机的最小距离
            double distanceSq = 0.0;
            // 计算XYZ轴方向的距离
            distanceSq += distanceSq(minX, maxX, cameraX, relX);
            distanceSq += distanceSq(minY, maxY, cameraY, relY);
            distanceSq += distanceSq(minZ, maxZ, cameraZ, relZ);
            // 检查距离是否超过最大值
            double maxDistanceSq = maxDistance * maxDistance;
            // 超过最大距离，剔除
            if (distanceSq > maxDistanceSq) {
                return false;
            }
        }

        if (!rayTracing || !cullable.rayTracing) {
            return true;
        }

        // 清空之前的缓存
        int dotMask = 0;
        this.canCheckLastHitBlock = false;
        if (relX == Relative.POSITIVE)      dotMask |= MASK_X_POS;
        else if (relX == Relative.NEGATIVE) dotMask |= MASK_X_NEG;
        if (relY == Relative.POSITIVE)      dotMask |= MASK_Y_POS;
        else if (relY == Relative.NEGATIVE) dotMask |= MASK_Y_NEG;
        if (relZ == Relative.POSITIVE)      dotMask |= MASK_Z_POS;
        else if (relZ == Relative.NEGATIVE) dotMask |= MASK_Z_NEG;

        int size = 0;
        if ((dotMask & (1 << 0)) != 0)  targetPoints[size++].set(minX, minY, minZ);
        if ((dotMask & (1 << 1)) != 0)  targetPoints[size++].set(maxX, minY, minZ);
        if ((dotMask & (1 << 2)) != 0)  targetPoints[size++].set(minX, minY, maxZ);
        if ((dotMask & (1 << 3)) != 0)  targetPoints[size++].set(maxX, minY, maxZ);
        if ((dotMask & (1 << 4)) != 0)  targetPoints[size++].set(minX, maxY, minZ);
        if ((dotMask & (1 << 5)) != 0)  targetPoints[size++].set(maxX, maxY, minZ);
        if ((dotMask & (1 << 6)) != 0)  targetPoints[size++].set(minX, maxY, maxZ);
        if ((dotMask & (1 << 7)) != 0)  targetPoints[size++].set(maxX, maxY, maxZ);

        double avgX = (minX + maxX) * 0.5;
        double avgY = (minY + maxY) * 0.5;
        double avgZ = (minZ + maxZ) * 0.5;

        if ((dotMask & (1 << 8)) != 0)  targetPoints[size++].set(avgX, avgY, minZ);
        if ((dotMask & (1 << 9)) != 0)  targetPoints[size++].set(avgX, avgY, maxZ);
        if ((dotMask & (1 << 10)) != 0) targetPoints[size++].set(minX, avgY, avgZ);
        if ((dotMask & (1 << 11)) != 0) targetPoints[size++].set(maxX, avgY, avgZ);
        if ((dotMask & (1 << 12)) != 0) targetPoints[size++].set(avgX, minY, avgZ);
        if ((dotMask & (1 << 13)) != 0) targetPoints[size++].set(avgX, maxY, avgZ);

//        if (Config.debugEntityCulling()) {
//            for (int i = 0; i < size; i++) {
//                MutableVec3d targetPoint = this.targetPoints[i];
//                this.player.playParticle(Key.of("flame"), targetPoint.x, targetPoint.y, targetPoint.z);
//            }
//        }

        return isVisible(cameraPos, this.targetPoints, size);
    }

    /**
     * 检测射线与轴对齐边界框(AABB)是否相交
     * 使用slab方法进行射线-AABB相交检测
     */
    private boolean rayIntersection(int x, int y, int z, MutableVec3d rayOrigin, MutableVec3d rayDirection) {
        // 计算射线方向的倒数，避免除法运算
        // 这对于处理射线方向分量为0的情况很重要
        double invDirX = 1.0 / rayDirection.x;
        double invDirY = 1.0 / rayDirection.y;
        double invDirZ = 1.0 / rayDirection.z;

        // 计算射线与边界框各对面（slab）的相交参数
        // 对于每个轴，计算射线进入和退出该轴对应两个平面的时间
        double tMinX = (x - rayOrigin.x) * invDirX;
        double tMaxX = (x + 1 - rayOrigin.x) * invDirX;
        double tMinY = (y - rayOrigin.y) * invDirY;
        double tMaxY = (y + 1 - rayOrigin.y) * invDirY;
        double tMinZ = (z - rayOrigin.z) * invDirZ;
        double tMaxZ = (z + 1 - rayOrigin.z) * invDirZ;

        // 计算射线进入边界框的最大时间（最近进入点）
        // 需要取各轴进入时间的最大值，因为射线必须进入所有轴的范围内
        double tEntry = Math.max(Math.max(Math.min(tMinX, tMaxX), Math.min(tMinY, tMaxY)), Math.min(tMinZ, tMaxZ));

        // 计算射线退出边界框的最短时间（最早退出点）
        // 需要取各轴退出时间的最小值，因为射线一旦退出任一轴的范围就离开了边界框
        double tExit = Math.min(Math.min(Math.max(tMinX, tMaxX), Math.max(tMinY, tMaxY)), Math.max(tMinZ, tMaxZ));

        // 如果最早退出时间大于0，说明整个边界框在射线起点后面
        // 这种情况我们视为不相交，因为通常我们只关心射线前方的相交
        if (tExit > 0) {
            return false;
        }

        // 如果进入时间大于退出时间，说明没有有效的相交区间
        // 这发生在射线完全错过边界框的情况下
        // 满足以下条件说明射线与边界框相交：
        // 1. 进入时间 <= 退出时间（存在有效相交区间）
        // 2. 退出时间 <= 0（边界框至少有一部分在射线起点前方或包含起点）
        return tEntry <= tExit;
    }

    /**
     * 使用3D DDA算法检测从起点到多个目标点的视线是否通畅
     * 算法基于数字微分分析，遍历射线路径上的所有方块
     */
    private boolean isVisible(Vec3d start, MutableVec3d[] targets, int targetCount) {

        // 起点所在方块的整数坐标（世界坐标转换为方块坐标）
        int startBlockX = MiscUtils.floor(start.x);
        int startBlockY = MiscUtils.floor(start.y);
        int startBlockZ = MiscUtils.floor(start.z);

        // 遍历所有目标点进行视线检测
        for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
            MutableVec3d currentTarget = targets[targetIndex];

            // 计算起点到目标的相对向量（世界坐标差）
            double deltaX = currentTarget.x - start.x;
            double deltaY = currentTarget.y - start.y;
            double deltaZ = currentTarget.z - start.z;

            // 检查之前命中的方块，大概率还是命中
            if (this.canCheckLastHitBlock) {
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                this.direction.set(deltaX / length, deltaY / length, deltaZ / length);
                if (rayIntersection(this.lastHitBlock[0], this.lastHitBlock[1], this.lastHitBlock[2], currentTarget, this.direction)) {
                    continue;
                }
            }

            // 计算相对向量的绝对值，用于确定各方向上的距离
            double absDeltaX = Math.abs(deltaX);
            double absDeltaY = Math.abs(deltaY);
            double absDeltaZ = Math.abs(deltaZ);

            // 预计算每单位距离在各方块边界上的步进增量
            // 这些值表示射线穿过一个方块所需的时间分数
            double stepIncrementX = 1.0 / absDeltaX;
            double stepIncrementY = 1.0 / absDeltaY;
            double stepIncrementZ = 1.0 / absDeltaZ;

            // 射线将穿过的总方块数量（包括起点和终点）
            int totalBlocksToCheck = 1;

            // 各方块坐标的步进方向（1: 正向, -1: 反向, 0: 静止）
            int stepDirectionX, stepDirectionY, stepDirectionZ;

            // 到下一个方块边界的时间参数（射线参数化表示）
            double nextStepTimeX, nextStepTimeY, nextStepTimeZ;

            // X方向步进参数计算
            if (absDeltaX == 0.0) {
                // X方向无变化，射线平行于YZ平面
                stepDirectionX = 0;
                nextStepTimeX = stepIncrementX;
            } else if (currentTarget.x > start.x) {
                // 目标在起点右侧，向右步进
                stepDirectionX = 1;
                totalBlocksToCheck += MiscUtils.floor(currentTarget.x) - startBlockX;
                nextStepTimeX = (startBlockX + 1 - start.x) * stepIncrementX;
            } else {
                // 目标在起点左侧，向左步进
                stepDirectionX = -1;
                totalBlocksToCheck += startBlockX - MiscUtils.floor(currentTarget.x);
                nextStepTimeX = (start.x - startBlockX) * stepIncrementX;
            }

            // Y方向步进参数计算
            if (absDeltaY == 0.0) {
                // Y方向无变化，射线平行于XZ平面
                stepDirectionY = 0;
                nextStepTimeY = stepIncrementY;
            } else if (currentTarget.y > start.y) {
                // 目标在起点上方，向上步进
                stepDirectionY = 1;
                totalBlocksToCheck += MiscUtils.floor(currentTarget.y) - startBlockY;
                nextStepTimeY = (startBlockY + 1 - start.y) * stepIncrementY;
            } else {
                // 目标在起点下方，向下步进
                stepDirectionY = -1;
                totalBlocksToCheck += startBlockY - MiscUtils.floor(currentTarget.y);
                nextStepTimeY = (start.y - startBlockY) * stepIncrementY;
            }

            // Z方向步进参数计算
            if (absDeltaZ == 0.0) {
                // Z方向无变化，射线平行于XY平面
                stepDirectionZ = 0;
                nextStepTimeZ = stepIncrementZ;
            } else if (currentTarget.z > start.z) {
                // 目标在起点前方，向前步进
                stepDirectionZ = 1;
                totalBlocksToCheck += MiscUtils.floor(currentTarget.z) - startBlockZ;
                nextStepTimeZ = (startBlockZ + 1 - start.z) * stepIncrementZ;
            } else {
                // 目标在起点后方，向后步进
                stepDirectionZ = -1;
                totalBlocksToCheck += startBlockZ - MiscUtils.floor(currentTarget.z);
                nextStepTimeZ = (start.z - startBlockZ) * stepIncrementZ;
            }

            // 执行DDA步进算法，遍历射线路径上的所有方块
            boolean isLineOfSightClear = stepRay(
                    startBlockX, startBlockY, startBlockZ,
                    stepIncrementX, stepIncrementY, stepIncrementZ, totalBlocksToCheck,
                    stepDirectionX, stepDirectionY, stepDirectionZ,
                    nextStepTimeY, nextStepTimeX, nextStepTimeZ);

            // 如果当前目标点可见立即返回
            if (isLineOfSightClear) {
                return true;
            } else {
                this.canCheckLastHitBlock = true;
            }
        }

        return false;
    }

    private boolean stepRay(int startingX, int startingY, int startingZ,
                            double stepSizeX, double stepSizeY, double stepSizeZ,
                            int remainingSteps,
                            int stepDirectionX, int stepDirectionY, int stepDirectionZ,
                            double nextStepTimeY, double nextStepTimeX, double nextStepTimeZ) {

        int currentBlockX = startingX;
        int currentBlockY = startingY;
        int currentBlockZ = startingZ;

        // 遍历射线路径上的所有方块
        for (; remainingSteps > 0; remainingSteps--) {

            // 检查当前方块是否遮挡视线
            if (isOccluding(currentBlockX, currentBlockY, currentBlockZ)) {
                this.lastHitBlock[0] = currentBlockX;
                this.lastHitBlock[1] = currentBlockY;
                this.lastHitBlock[2] = currentBlockZ;
                return false; // 视线被遮挡，立即返回
            }

            // 基于时间参数选择下一个要遍历的方块方向
            // 选择距离最近的方块边界作为下一步
            if (nextStepTimeY < nextStepTimeX && nextStepTimeY < nextStepTimeZ) {
                // Y方向边界最近，垂直移动
                currentBlockY += stepDirectionY;
                nextStepTimeY += stepSizeY;
            } else if (nextStepTimeX < nextStepTimeY && nextStepTimeX < nextStepTimeZ) {
                // X方向边界最近，水平移动
                currentBlockX += stepDirectionX;
                nextStepTimeX += stepSizeX;
            } else {
                // Z方向边界最近，深度移动
                currentBlockZ += stepDirectionZ;
                nextStepTimeZ += stepSizeZ;
            }
        }

        // 成功遍历所有中间方块，视线通畅
        return true;
    }

    private int getCacheIndex(int x, int y, int z, int startX, int startY, int startZ) {
        int deltaX = startX + 16 - x;
        if (deltaX < 0 || deltaX >= 32) {
            return -1;
        }
        int deltaY = startY + 16 - y;
        if (deltaY < 0 || deltaY >= 32) {
            return -1;
        }
        int deltaZ = startZ + 16 - z;
        if (deltaZ < 0 || deltaZ >= 32) {
            return -1;
        }
        return deltaX + 32 * deltaY + 32 * 32 * deltaZ;
    }

    private double distanceSq(double min, double max, double camera, Relative rel) {
        if (rel == Relative.NEGATIVE) {
            double dx = camera - max;
            return dx * dx;
        } else if (rel == Relative.POSITIVE) {
            double dx = min - camera;
            return dx * dx;
        }
        return 0d;
    }

    private boolean isOccluding(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ClientChunk trackedChunk;
        // 使用上次记录的值，比每次走hash都更快
        if (chunkX == this.lastVisitChunkX && chunkZ == this.lastVisitChunkZ) {
            trackedChunk = this.lastVisitChunk;
        } else {
            trackedChunk = this.player.getTrackedChunk(ChunkPos.asLong(chunkX, chunkZ));
            this.lastVisitChunk = trackedChunk;
            this.lastVisitChunkX = chunkX;
            this.lastVisitChunkZ = chunkZ;
        }
        if (trackedChunk == null) {
            return false;
        }
        return trackedChunk.isOccluding(x, y, z);
    }

    public void removeLastVisitChunkIfMatches(int chunkX, int chunkZ) {
        if (this.lastVisitChunk != null && this.lastVisitChunkX == chunkX && this.lastVisitChunkZ == chunkZ) {
            this.lastVisitChunk = null;
        }
    }

    private enum Relative {
        INSIDE, POSITIVE, NEGATIVE;
        public static Relative from(double min, double max, double pos) {
            if (min > pos) return POSITIVE;
            else if (max < pos) return NEGATIVE;
            return INSIDE;
        }
    }
}
