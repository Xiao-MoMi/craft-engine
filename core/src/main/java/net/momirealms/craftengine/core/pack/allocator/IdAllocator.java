package net.momirealms.craftengine.core.pack.allocator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public final class IdAllocator {
    private final Path cacheFilePath;
    private final BitSet occupiedIdSet = new BitSet();
    private final Map<String, CompletableFuture<Integer>> pendingAllocations = new LinkedHashMap<>();
    private final Map<String, Integer> cachedIdMap = new LinkedHashMap<>();
    private final List<CompletableFuture<?>> combinedFutures = new ArrayList<>();

    private final BiMap<String, Integer> forcedIdMap = HashBiMap.create(128);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private long lastModified;

    private int nextAutoId;
    private int minId;
    private int maxId;

    private boolean dirty;

    public IdAllocator(Path cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }

    /**
     * 重置分配器状态
     * @param minId 最小ID（包含）
     * @param maxId 最大ID（包含）
     */
    public void reset(int minId, int maxId) {
        this.minId = minId;
        this.nextAutoId = minId;
        this.maxId = maxId;
        this.occupiedIdSet.clear();
        this.forcedIdMap.clear();
        this.pendingAllocations.clear();
        this.combinedFutures.clear();
    }

    public List<CompletableFuture<?>> combinedFutures() {
        return combinedFutures;
    }

    public synchronized void addCombinedFuture(@NotNull CompletableFuture<?> future) {
        this.combinedFutures.add(future);
    }

    /**
     * 处理所有待分配的自动ID请求
     */
    public void processPendingAllocations() {
        for (Map.Entry<String, Integer> entry : this.cachedIdMap.entrySet()) {
            CompletableFuture<Integer> future = this.pendingAllocations.get(entry.getKey());
            if (future != null) {
                int id = entry.getValue();
                if (!isIdAvailable(id)) {
                    continue;
                }
                allocateId(id, future);
            } else {
                // 避免其他条目分配到过时的值上
                this.occupiedIdSet.set(entry.getValue());
            }
        }

        for (Map.Entry<String, CompletableFuture<Integer>> entry : this.pendingAllocations.entrySet()) {
            String name = entry.getKey();
            CompletableFuture<Integer> future = entry.getValue();

            if (future.isDone()) {
                continue; // 已经在前面分配过了
            }

            // 分配新的自动ID
            int newId = findNextAvailableId();
            if (newId == -1) {
                future.completeExceptionally(new IdExhaustedException(name, this.minId, this.maxId));
                continue;
            }

            allocateId(newId, future);
            this.cachedIdMap.put(name, newId);
            this.dirty = true;
        }

        this.pendingAllocations.clear();
    }

    private boolean isIdAvailable(Integer id) {
        return id != null && id >= this.minId && id <= this.maxId
                && !this.occupiedIdSet.get(id);
    }

    private void allocateId(int id, CompletableFuture<Integer> future) {
        this.occupiedIdSet.set(id);
        future.complete(id);
    }

    private int findNextAvailableId() {
        if (this.nextAutoId > this.maxId) {
            return -1;
        }

        this.nextAutoId = this.occupiedIdSet.nextClearBit(this.nextAutoId);
        return this.nextAutoId <= this.maxId ? this.nextAutoId : -1;
    }

    /**
     * 强制分配指定ID，无视限制
     * @param name 名称
     * @param id 要分配的ID
     * @return 分配结果的Future
     */
    public CompletableFuture<Integer> assignFixedId(String name, int id) {
        try {
            this.writeLock.lock();
            // 检查ID是否被其他名称占用
            String existingOwner = this.forcedIdMap.inverse().get(id);
            if (existingOwner != null && !existingOwner.equals(name)) {
                return CompletableFuture.failedFuture(new CompletionException(new IdConflictException(existingOwner, id)));
            }

            this.forcedIdMap.put(name, id);
            this.cachedIdMap.remove(name); // 清除可能的缓存
            this.occupiedIdSet.set(id);
            return CompletableFuture.completedFuture(id);
        } finally {
            this.writeLock.unlock();
        }
    }

    public boolean isForced(String name) {
        try {
            this.readLock.lock();
            return this.forcedIdMap.containsKey(name);
        } finally {
            this.readLock.unlock();
        }
    }

    public List<Pair<String, Integer>> getFixedIdsBetween(int minId, int maxId) {
        try {
            this.readLock.lock();
            BiMap<Integer, String> inverse = this.forcedIdMap.inverse();
            List<Pair<String, Integer>> result = new ArrayList<>();
            for (int i = minId; i <= maxId; i++) {
                String s = inverse.get(i);
                if (s != null) {
                    result.add(Pair.of(s, i));
                }
            }
            return result;
        } finally {
            this.readLock.unlock();
        }
    }

    /**
     * 请求自动分配ID
     * @param name 名称
     * @return 分配结果的Future
     */
    public synchronized CompletableFuture<Integer> requestAutoId(String name) {
        CompletableFuture<Integer> previousFuture = this.pendingAllocations.get(name);
        if (previousFuture != null) {
            return previousFuture;
        }
        CompletableFuture<Integer> future = new CompletableFuture<>();
        this.pendingAllocations.put(name, future);
        return future;
    }

    /**
     * 清理不再使用的ID
     * @param shouldRemove 判断是否应该移除的谓词
     * @return 被移除的ID数量
     */
    public List<String> cleanupUnusedIds(Predicate<String> shouldRemove) {
        List<String> idsToRemove = new ArrayList<>();
        for (String id : this.cachedIdMap.keySet()) {
            if (shouldRemove.test(id)) {
                idsToRemove.add(id);
            }
        }

        if (!idsToRemove.isEmpty()) {
            this.dirty = true;
            for (String id : idsToRemove) {
                Integer removedId = this.cachedIdMap.remove(id);
                if (removedId != null && !this.forcedIdMap.containsValue(removedId)) {
                    this.occupiedIdSet.clear(removedId);
                }
            }
        }

        return idsToRemove;
    }

    @NotNull
    public Map<String, Integer> cachedIdMap() {
        return Collections.unmodifiableMap(this.cachedIdMap);
    }

    /**
     * 检查ID是否已被占用
     */
    public boolean isIdOccupied(int id) {
        return this.occupiedIdSet.get(id);
    }

    /**
     * 从文件加载缓存
     */
    public void loadFromCache() throws IOException {
        if (!Files.exists(this.cacheFilePath)) {
            if (!this.cachedIdMap.isEmpty()) {
                this.cachedIdMap.clear();
            }
            return;
        }

        long lastTime = Files.getLastModifiedTime(this.cacheFilePath).toMillis();
        if (lastTime != this.lastModified) {
            this.lastModified = lastTime;
            this.cachedIdMap.clear();
            JsonElement element = GsonHelper.readJsonFromFile(this.cacheFilePath);
            if (element instanceof JsonObject jsonObject) {
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    if (entry.getValue() instanceof JsonPrimitive primitive) {
                        int id = primitive.getAsInt();
                        this.cachedIdMap.put(entry.getKey(), id);
                    }
                }
            }
        }
    }

    /**
     * 保存缓存到文件
     */
    public void saveToCache() throws IOException {
        // 如果没有更改
        if (!this.dirty) {
            return;
        }

        this.dirty = false;

        // 创建按ID排序的TreeMap
        Map<Integer, String> sortedById = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : this.cachedIdMap.entrySet()) {
            sortedById.put(entry.getValue(), entry.getKey());
        }

        // 创建有序的JSON对象
        JsonObject sortedJsonObject = new JsonObject();
        for (Map.Entry<Integer, String> entry : sortedById.entrySet()) {
            sortedJsonObject.addProperty(entry.getValue(), entry.getKey());
        }

        if (sortedJsonObject.asMap().isEmpty()) {
            if (Files.exists(this.cacheFilePath)) {
                Files.delete(this.cacheFilePath);
            }
        } else {
            FileUtils.createDirectoriesSafe(this.cacheFilePath.getParent());
            GsonHelper.writeJsonFile(sortedJsonObject, this.cacheFilePath);
        }
    }

    public static class IdConflictException extends RuntimeException {
        private final String previousOwner;
        private final int id;

        public IdConflictException(String previousOwner, int id) {
            super("ID " + id + " is already occupied by: " + previousOwner);
            this.previousOwner = previousOwner;
            this.id = id;
        }

        public String previousOwner() {
            return previousOwner;
        }

        public int id() {
            return id;
        }
    }

    public static class IdExhaustedException extends RuntimeException {
        private final String name;
        private final int min;
        private final int max;

        public IdExhaustedException(String name, int min, int max) {
            super("No available auto ID for '" + name + "'. All IDs in range " + min + "-" + max + " are occupied.");
            this.name = name;
            this.min = min;
            this.max = max;
        }

        public String name() {
            return name;
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }
    }
}