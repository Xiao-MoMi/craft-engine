package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.world.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FurnitureLightData {
    private final ReentrantReadWriteLock lightLock = new ReentrantReadWriteLock();
    private final Map<BlockPos, int[]> furnitureLightData = new HashMap<>();

    public int addLightData(BlockPos pos, int lightPower) {
        ReentrantReadWriteLock.WriteLock writeLock = lightLock.writeLock();
        try {
            writeLock.lock();
            int[] lights = this.furnitureLightData.get(pos);
            // 没有数据则创建数据, 当前值就是最大值.
            if (lights == null) {
                this.furnitureLightData.put(pos, new int[] {lightPower});
                return lightPower;
            }
            // 如果比第一个大, 当前值就是最大值.
            if (lightPower > lights[0]) {
                int[] ints = ArrayUtils.appendIntToArrayHead(lights, lightPower);
                this.furnitureLightData.put(pos, ints);
                return lightPower;
            }
            // 如果比第一个元素小/相等, 并且只有一个元素, 直接组合返回.
            else if (lights.length == 1) {
                this.furnitureLightData.put(pos, new int[] {lights[0], lightPower});
                return -1; // 不操作
            }
            // 如果比第一个小, 则插入到合适的位置, 然后返回 0 代表不更新光照.
            int[] newLights = new int[lights.length + 1];
            boolean inserted = false;
            newLights[0] = lights[0];  // 头部最大值保持不变

            // 从1开始遍历旧数组插入新光照
            int j = 1;
            for (int i = 1; i < lights.length; i++) {
                int current = lights[i];
                if (!inserted && lightPower > current) {
                    newLights[j++] = lightPower;
                    inserted = true;
                }
                newLights[j++] = lights[i];
            }

            // lightPower比所有值都小, 放末尾
            if (!inserted) {
                newLights[j] = lightPower;
            }

            this.furnitureLightData.put(pos, newLights);
            return -1; // 不操作
        } finally {
            writeLock.unlock();
        }
    }

    public int removeLightData(BlockPos pos, int lightPower) {
        ReentrantReadWriteLock.WriteLock writeLock = lightLock.writeLock();
        try {
            writeLock.lock();
            int[] lights = this.furnitureLightData.get(pos);
            // 没有数据，无法移除
            if (lights == null || lights.length == 0) {
                return -1;
            }

            // 找到要移除的光照值索引
            int removeIndex = -1;
            for (int i = 0; i < lights.length; i++) {
                if (lights[i] == lightPower) {
                    removeIndex = i;
                    break;
                }
            }
            // 没有找到光照，返回-1
            if (removeIndex == -1) {
                return -1;
            }

            // 长度为1, 直接则移除整个条目
            if (lights.length == 1) {
                this.furnitureLightData.remove(pos);
                return 0; // 当前位置无光照了, 返回0
            }

            // 创建新数组，复制除removeIndex外的其他元素到新数组.
            int[] newLights = new int[lights.length - 1];
            System.arraycopy(lights, 0, newLights, 0, removeIndex);
            if (removeIndex < lights.length - 1) {
                System.arraycopy(lights, removeIndex + 1, newLights, removeIndex, lights.length - removeIndex - 1);
            }
            this.furnitureLightData.put(pos, newLights);

            // 如果移除的是头部, 代表移除的是最大光照.
            // 检查最新的最大光照是否和旧的最大光照相同, 如果不同则需要返回最新的最大光照.
            if (removeIndex == 0 && lights[0] != newLights[0]) {
                return newLights[0]; // 新的最大光照
            }

            return -1; // 什么都不操作
        } finally {
            writeLock.unlock();
        }
    }

    public void clearLightData() {
        ReentrantReadWriteLock.WriteLock writeLock = lightLock.writeLock();
        try {
            writeLock.lock();
            this.furnitureLightData.clear();
        } finally {
            writeLock.unlock();
        }
    }

    public int getLightPower(BlockPos pos) {
        ReentrantReadWriteLock.ReadLock readLock = lightLock.readLock();
        try {
            readLock.lock();
            int[] ints = this.furnitureLightData.get(pos);
            if (ints == null) return 0;
            return ints[0];
        } finally {
            readLock.unlock();
        }
    }
}
