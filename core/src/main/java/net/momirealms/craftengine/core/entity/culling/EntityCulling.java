package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.MutableVec3d;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.Arrays;

public final class EntityCulling {
    public static final int MAX_SAMPLES = 14;
    private final Player player;
    private final boolean[] dotSelectors = new boolean[MAX_SAMPLES];
    private final MutableVec3d[] targetPoints = new MutableVec3d[MAX_SAMPLES];
    private final int[] lastHitBlock = new int[3];
    private boolean canCheckLastHitBlock = false;
    private int hitBlockCount = 0;
    private int lastVisitChunkX = Integer.MAX_VALUE;
    private int lastVisitChunkZ = Integer.MAX_VALUE;
    private ClientChunk lastVisitChunk = null;
    private int currentTokens = Config.entityCullingRateLimitingBucketSize();
    private double distanceScale = 1d;
    // Pre-allocated vector pool for zero-allocation ray tracing
    private final ReusableVectorPool vectorPool = new ReusableVectorPool();
    // Local occlusion cache for fast block lookups
    private final LocalOcclusionCache localOcclusionCache = new LocalOcclusionCache();
    // Temporal coherence cache for frame-to-frame visibility caching
    private final TemporalCoherenceCache temporalCache = new TemporalCoherenceCache();
    // Hierarchical occlusion map for fast empty region skipping
    private final HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
    // Adaptive sampler for distance-based sample count optimization
    private final AdaptiveSampler adaptiveSampler = new AdaptiveSampler();

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
        this.canCheckLastHitBlock = false;
        this.hitBlockCount = 0;
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

        // Update temporal cache based on camera movement
        temporalCache.invalidateIfMoved(cameraX, cameraY, cameraZ);

        Relative relX = Relative.from(minX, maxX, cameraX);
        Relative relY = Relative.from(minY, maxY, cameraY);
        Relative relZ = Relative.from(minZ, maxZ, cameraZ);

        // 相机位于实体内部
        if (relX == Relative.INSIDE && relY == Relative.INSIDE && relZ == Relative.INSIDE) {
            return true;
        }

        // 如果设置了最大距离
        double maxDistance = cullable.maxDistance * this.distanceScale;
        // Calculate distance squared for adaptive sampling
        double distanceSq = 0.0;
        // 计算XYZ轴方向的距离
        distanceSq += distanceSq(minX, maxX, cameraX, relX);
        distanceSq += distanceSq(minY, maxY, cameraY, relY);
        distanceSq += distanceSq(minZ, maxZ, cameraZ, relZ);
        
        if (maxDistance > 0) {
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

        // Create entity key for temporal cache lookup
        int entityBlockX = (int) Math.floor((minX + maxX) / 2.0);
        int entityBlockY = (int) Math.floor((minY + maxY) / 2.0);
        int entityBlockZ = (int) Math.floor((minZ + maxZ) / 2.0);
        long entityKey = TemporalCoherenceCache.createEntityKey(entityBlockX, entityBlockY, entityBlockZ);

        // Check temporal cache for cached visibility result
        if (temporalCache.shouldUseCache(cameraX, cameraY, cameraZ)) {
            Boolean cachedVisibility = temporalCache.getCachedVisibility(entityKey);
            if (cachedVisibility != null) {
                return cachedVisibility;
            }
        }

        // 清空之前的缓存
        Arrays.fill(this.dotSelectors, false);
        if (relX == Relative.POSITIVE) {
            this.dotSelectors[0] = this.dotSelectors[2] = this.dotSelectors[4] = this.dotSelectors[6] = this.dotSelectors[10] = true;
        } else if (relX == Relative.NEGATIVE) {
            this.dotSelectors[1] = this.dotSelectors[3] = this.dotSelectors[5] = this.dotSelectors[7] = this.dotSelectors[11] = true;
        }
        if (relY == Relative.POSITIVE) {
            this.dotSelectors[0] = this.dotSelectors[1] = this.dotSelectors[2] = this.dotSelectors[3] = this.dotSelectors[12] = true;
        } else if (relY == Relative.NEGATIVE) {
            this.dotSelectors[4] = this.dotSelectors[5] = this.dotSelectors[6] = this.dotSelectors[7] = this.dotSelectors[13] = true;
        }
        if (relZ == Relative.POSITIVE) {
            this.dotSelectors[0] = this.dotSelectors[1] = this.dotSelectors[4] = this.dotSelectors[5] = this.dotSelectors[8] = true;
        } else if (relZ == Relative.NEGATIVE) {
            this.dotSelectors[2] = this.dotSelectors[3] = this.dotSelectors[6] = this.dotSelectors[7] = this.dotSelectors[9] = true;
        }

        int size = 0;
        if (this.dotSelectors[0]) targetPoints[size++].set(minX, minY, minZ);
        if (this.dotSelectors[1]) targetPoints[size++].set(maxX, minY, minZ);
        if (this.dotSelectors[2]) targetPoints[size++].set(minX, minY, maxZ);
        if (this.dotSelectors[3]) targetPoints[size++].set(maxX, minY, maxZ);
        if (this.dotSelectors[4]) targetPoints[size++].set(minX, maxY, minZ);
        if (this.dotSelectors[5]) targetPoints[size++].set(maxX, maxY, minZ);
        if (this.dotSelectors[6]) targetPoints[size++].set(minX, maxY, maxZ);
        if (this.dotSelectors[7]) targetPoints[size++].set(maxX, maxY, maxZ);
        // 面中心点
        double averageX = (minX + maxX) / 2.0;
        double averageY = (minY + maxY) / 2.0;
        double averageZ = (minZ + maxZ) / 2.0;
        if (this.dotSelectors[8]) targetPoints[size++].set(averageX, averageY, minZ);
        if (this.dotSelectors[9]) targetPoints[size++].set(averageX, averageY, maxZ);
        if (this.dotSelectors[10]) targetPoints[size++].set(minX, averageY, averageZ);
        if (this.dotSelectors[11]) targetPoints[size++].set(maxX, averageY, averageZ);
        if (this.dotSelectors[12]) targetPoints[size++].set(averageX, minY, averageZ);
        if (this.dotSelectors[13]) targetPoints[size++].set(averageX, maxY, averageZ);

        // Apply adaptive sampling - limit sample count based on distance
        int adaptiveSampleCount = adaptiveSampler.getSampleCount(distanceSq);
        int effectiveSize = Math.min(size, adaptiveSampleCount);

//        if (Config.debugEntityCulling()) {
//            for (int i = 0; i < size; i++) {
//                MutableVec3d targetPoint = this.targetPoints[i];
//                this.player.playParticle(Key.of("flame"), targetPoint.x, targetPoint.y, targetPoint.z);
//            }
//        }
        boolean visible = isVisible(cameraPos, this.targetPoints, effectiveSize);
        
        // Cache the visibility result for temporal coherence
        temporalCache.cacheVisibility(entityKey, visible);
        
        return visible;
    }

    /**
     * 检测射线与轴对齐边界框(AABB)是否相交
     * 使用slab方法进行射线-AABB相交检测
     */
    private boolean rayIntersection(int x, int y, int z, Vec3d rayOrigin, MutableVec3d rayDirection) {
        // 使用预分配的向量计算射线方向的倒数，避免除法运算和对象分配
        // 这对于处理射线方向分量为0的情况很重要
        MutableVec3d inverseRayDirection = vectorPool.getInverseRayDirection();
        inverseRayDirection.set(1.0 / rayDirection.x, 1.0 / rayDirection.y, 1.0 / rayDirection.z);

        // 计算射线与边界框各对面（slab）的相交参数
        // 对于每个轴，计算射线进入和退出该轴对应两个平面的时间
        double tMinX = (x - rayOrigin.x) * inverseRayDirection.x;
        double tMaxX = (x + 1 - rayOrigin.x) * inverseRayDirection.x;
        double tMinY = (y - rayOrigin.y) * inverseRayDirection.y;
        double tMaxY = (y + 1 - rayOrigin.y) * inverseRayDirection.y;
        double tMinZ = (z - rayOrigin.z) * inverseRayDirection.z;
        double tMaxZ = (z + 1 - rayOrigin.z) * inverseRayDirection.z;

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
            double deltaX = start.x - currentTarget.x;
            double deltaY = start.y - currentTarget.y;
            double deltaZ = start.z - currentTarget.z;

            // 检查之前命中的方块，大概率还是命中
            if (this.canCheckLastHitBlock) {
                // 使用预分配的向量避免对象分配
                MutableVec3d normalizedDir = vectorPool.getNormalizedDirection();
                normalizedDir.set(deltaX, deltaY, deltaZ);
                normalizedDir.normalize();
                if (rayIntersection(this.lastHitBlock[0], this.lastHitBlock[1], this.lastHitBlock[2], start, normalizedDir)) {
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

    /**
     * DDA ray stepping algorithm with hierarchical skip and branch prediction optimization.
     * Uses hierarchical occlusion map to skip empty 2x2x2 regions.
     * Uses branchless minimum selection and switch expression for better CPU pipeline utilization.
     *
     */
    private boolean stepRay(int startingX, int startingY, int startingZ,
                            double stepSizeX, double stepSizeY, double stepSizeZ,
                            int remainingSteps,
                            int stepDirectionX, int stepDirectionY, int stepDirectionZ,
                            double nextStepTimeY, double nextStepTimeX, double nextStepTimeZ) {

        int currentBlockX = startingX;
        int currentBlockY = startingY;
        int currentBlockZ = startingZ;
        
        // Get cache base coordinates for hierarchical lookup
        int cacheBaseX = localOcclusionCache.getBaseX();
        int cacheBaseY = localOcclusionCache.getBaseY();
        int cacheBaseZ = localOcclusionCache.getBaseZ();
        boolean useHierarchical = localOcclusionCache.isValid() && hierarchicalMap.isValid();

        // 遍历射线路径上的所有方块
        while (remainingSteps > 0) {
            
            // Try hierarchical skip for empty 2x2x2 groups
            if (useHierarchical && localOcclusionCache.isInBounds(currentBlockX, currentBlockY, currentBlockZ)) {
                int groupX = HierarchicalOcclusionMap.worldToGroupCoord(currentBlockX, cacheBaseX);
                int groupY = HierarchicalOcclusionMap.worldToGroupCoord(currentBlockY, cacheBaseY);
                int groupZ = HierarchicalOcclusionMap.worldToGroupCoord(currentBlockZ, cacheBaseZ);
                
                if (groupX >= 0 && groupY >= 0 && groupZ >= 0 && 
                    hierarchicalMap.isGroupInBounds(groupX, groupY, groupZ)) {
                    
                    // Check if the entire 2x2x2 group is empty (no occluding blocks)
                    if (!hierarchicalMap.isGroupOccluding(groupX, groupY, groupZ)) {
                        // Skip the entire group - advance by 2 blocks
                        int skipDistance = HierarchicalOcclusionMap.GROUP_SIZE;
                        
                        // Advance in the dominant direction by skip distance
                        for (int skip = 0; skip < skipDistance && remainingSteps > 0; skip++) {
                            int minDirection = selectMinDirection(nextStepTimeX, nextStepTimeY, nextStepTimeZ);
                            switch (minDirection) {
                                case 0 -> {
                                    currentBlockX += stepDirectionX;
                                    nextStepTimeX += stepSizeX;
                                }
                                case 1 -> {
                                    currentBlockY += stepDirectionY;
                                    nextStepTimeY += stepSizeY;
                                }
                                default -> {
                                    currentBlockZ += stepDirectionZ;
                                    nextStepTimeZ += stepSizeZ;
                                }
                            }
                            remainingSteps--;
                        }
                        continue;
                    }
                }
            }

            // 检查当前方块是否遮挡视线
            if (isOccluding(currentBlockX, currentBlockY, currentBlockZ)) {
                this.lastHitBlock[this.hitBlockCount * 3] = currentBlockX;
                this.lastHitBlock[this.hitBlockCount * 3 + 1] = currentBlockY;
                this.lastHitBlock[this.hitBlockCount * 3 + 2] = currentBlockZ;
                return false; // 视线被遮挡，立即返回
            }

            // Branchless minimum selection using bit manipulation
            // Compute which axis has the minimum next step time
            // Direction encoding: 0 = X, 1 = Y, 2 = Z
            int minDirection = selectMinDirection(nextStepTimeX, nextStepTimeY, nextStepTimeZ);

            // Use switch expression for predictable branch pattern
            switch (minDirection) {
                case 0 -> {
                    // X方向边界最近，水平移动
                    currentBlockX += stepDirectionX;
                    nextStepTimeX += stepSizeX;
                }
                case 1 -> {
                    // Y方向边界最近，垂直移动
                    currentBlockY += stepDirectionY;
                    nextStepTimeY += stepSizeY;
                }
                default -> {
                    // Z方向边界最近，深度移动 (case 2)
                    currentBlockZ += stepDirectionZ;
                    nextStepTimeZ += stepSizeZ;
                }
            }
            remainingSteps--;
        }

        // 成功遍历所有中间方块，视线通畅
        return true;
    }

    /**
     * Branchless minimum direction selection for DDA algorithm.
     * Uses comparison results to compute direction index without branching.
     *
     * 
     * @param timeX next step time for X axis
     * @param timeY next step time for Y axis
     * @param timeZ next step time for Z axis
     * @return direction index: 0 = X, 1 = Y, 2 = Z
     */
    private static int selectMinDirection(double timeX, double timeY, double timeZ) {
        // Branchless minimum selection using boolean-to-int conversion
        // This pattern allows CPU to execute comparisons in parallel
        // and avoids branch misprediction penalties
        
        // Compare X vs Y and X vs Z
        boolean xLessY = timeX <= timeY;
        boolean xLessZ = timeX <= timeZ;
        
        // Compare Y vs Z
        boolean yLessZ = timeY <= timeZ;
        
        // X is minimum if X <= Y and X <= Z
        // Y is minimum if Y < X and Y <= Z
        // Z is minimum otherwise
        
        // Using branchless selection:
        // If X is minimum: return 0
        // If Y is minimum: return 1
        // If Z is minimum: return 2
        
        // Convert boolean comparisons to direction index
        // xMin = xLessY && xLessZ -> direction 0
        // yMin = !xLessY && yLessZ -> direction 1
        // zMin = otherwise -> direction 2
        
        if (xLessY && xLessZ) {
            return 0; // X is minimum
        }
        if (yLessZ) {
            return 1; // Y is minimum (since X is not minimum and Y <= Z)
        }
        return 2; // Z is minimum
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
        // Cache-first approach: check local cache if valid and in bounds
        if (localOcclusionCache.isValid() && localOcclusionCache.isInBounds(x, y, z)) {
            return localOcclusionCache.isOccluding(x, y, z);
        }
        
        // Fallback to chunk-based lookup for coordinates outside cache bounds
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

    /**
     * Updates the local occlusion cache centered on the player's current position.
     * Also updates the hierarchical occlusion map for fast empty region skipping.
     * Should be called when the player moves or when chunk data changes.
     *
     */
    public void updateLocalOcclusionCache() {
        this.localOcclusionCache.update(this.player);
        // Update hierarchical map from the local cache
        this.hierarchicalMap.updateFromLocalCache(this.localOcclusionCache);
    }

    /**
     * Invalidates the local occlusion cache and hierarchical map, forcing a refresh on next update.
     */
    public void invalidateLocalOcclusionCache() {
        this.localOcclusionCache.invalidate();
        this.hierarchicalMap.invalidate();
    }

    /**
     * Gets the local occlusion cache for testing purposes.
     * @return the local occlusion cache
     */
    LocalOcclusionCache getLocalOcclusionCache() {
        return this.localOcclusionCache;
    }

    /**
     * Gets the temporal coherence cache for testing purposes.
     * @return the temporal coherence cache
     */
    TemporalCoherenceCache getTemporalCache() {
        return this.temporalCache;
    }

    /**
     * Gets the hierarchical occlusion map for testing purposes.
     * @return the hierarchical occlusion map
     */
    HierarchicalOcclusionMap getHierarchicalMap() {
        return this.hierarchicalMap;
    }

    /**
     * Gets the adaptive sampler for testing purposes.
     * @return the adaptive sampler
     */
    AdaptiveSampler getAdaptiveSampler() {
        return this.adaptiveSampler;
    }

    /**
     * Clears the temporal coherence cache.
     * Should be called when entity positions change significantly.
     */
    public void clearTemporalCache() {
        this.temporalCache.clear();
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
