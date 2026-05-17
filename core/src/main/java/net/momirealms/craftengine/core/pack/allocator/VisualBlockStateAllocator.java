package net.momirealms.craftengine.core.pack.allocator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.block.AutoStateGroup;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.util.CompletableFutures;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

public final class VisualBlockStateAllocator {
    private final Path cacheFilePath;
    private final Map<String, BlockStateWrapper> cachedBlockStates = new LinkedHashMap<>();
    private final Map<String, Pair<AutoStateGroup, CompletableFuture<BlockStateWrapper>>> pendingAllocations = new LinkedHashMap<>();
    @SuppressWarnings("unchecked")
    private final List<Pair<String, CompletableFuture<BlockStateWrapper>>>[] pendingAllocationFutures = new List[AutoStateGroup.values().length];
    private final BlockStateCandidate[] candidates;
    private final Function<String, BlockStateWrapper> factory;
    private final List<CompletableFuture<?>> combinedFutures = new ArrayList<>();
    private final Set<BlockStateWrapper> forcedStates = new HashSet<>();
    private boolean dirty;
    private long lastModified;

    public VisualBlockStateAllocator(Path cacheFilePath, BlockStateCandidate[] candidates, Function<String, BlockStateWrapper> factory) {
        this.cacheFilePath = cacheFilePath;
        this.candidates = candidates;
        this.factory = factory;
    }

    public void reset() {
        for (int i = 0; i < this.pendingAllocationFutures.length; i++) {
            this.pendingAllocationFutures[i] = new ArrayList<>();
        }
        this.pendingAllocations.clear();
        this.forcedStates.clear();
        this.combinedFutures.clear();
    }

    public CompletableFuture<Void> combinedFuture() {
        return CompletableFutures.allOf(this.combinedFutures);
    }

    public List<CompletableFuture<?>> combinedFutures() {
        return this.combinedFutures;
    }

    public synchronized void addCombinedFuture(@Nullable CompletableFuture<Void> future) {
        this.combinedFutures.add(future);
    }

    public boolean isForcedState(final BlockStateWrapper state) {
        return this.forcedStates.contains(state);
    }

    @NotNull
    public Map<String, BlockStateWrapper> cachedBlockStates() {
        return Collections.unmodifiableMap(this.cachedBlockStates);
    }

    public synchronized CompletableFuture<BlockStateWrapper> assignFixedBlockState(String name, BlockStateWrapper state) {
        this.cachedBlockStates.remove(name);
        this.forcedStates.add(state);
        BlockStateCandidate candidate = this.candidates[state.registryId()];
        if (candidate != null) {
            candidate.setUsed();
        }
        return CompletableFuture.completedFuture(state);
    }

    public synchronized CompletableFuture<BlockStateWrapper> requestAutoState(String name, AutoStateGroup group) {
        if (this.pendingAllocations.containsKey(name)) {
            return this.pendingAllocations.get(name).right();
        }
        CompletableFuture<BlockStateWrapper> future = new CompletableFuture<>();
        this.pendingAllocations.put(name, new Pair<>(group, future));
        this.pendingAllocationFutures[group.ordinal()].add(Pair.of(name, future));
        return future;
    }

    public List<String> cleanupUnusedIds(Predicate<BlockStateWrapper> shouldRemove) {
        List<String> idsToRemove = new ArrayList<>();
        for (Map.Entry<String, BlockStateWrapper> entry : this.cachedBlockStates.entrySet()) {
            if (shouldRemove.test(entry.getValue())) {
                idsToRemove.add(entry.getKey());
            }
        }
        if (!idsToRemove.isEmpty()) {
            this.dirty = true;
            for (String id : idsToRemove) {
                this.cachedBlockStates.remove(id);
            }
        }
        return idsToRemove;
    }

    public void processPendingAllocations() {
        // 先处理缓存的
        for (Map.Entry<String, BlockStateWrapper> entry : this.cachedBlockStates.entrySet()) {
            int registryId = entry.getValue().registryId();
            // 检查候选方块是否可用
            BlockStateCandidate candidate = this.candidates[registryId];
            if (candidate != null) {
                // 未被使用
                if (!candidate.isUsed()) {
                    // 获取当前的安排任务
                    Pair<AutoStateGroup, CompletableFuture<BlockStateWrapper>> pair = this.pendingAllocations.get(entry.getKey());
                    if (pair != null) {
                        // 如果候选满足组，那么直接允许起飞
                        if (pair.left().test(candidate.blockState())) {
                            pair.right().complete(candidate.blockState());
                            candidate.setUsed();
                        } else {
                            // 不满足候选组要求，那就等着分配新的吧
                        }
                    } else {
                        // 尽管未被使用，该槽位也应该被占用，以避免被自动分配到
                        candidate.setUsed();
                    }
                }
                // 被使用了就随他去
            }
            // 没有候选也随他去
        }

        this.pendingAllocations.clear();

        for (AutoStateGroup group : AutoStateGroup.values()) {
            List<Pair<String, CompletableFuture<BlockStateWrapper>>> pendingAllocationFuture = this.pendingAllocationFutures[group.ordinal()];
            for (Pair<String, CompletableFuture<BlockStateWrapper>> pair : pendingAllocationFuture) {
                if (!pair.right().isDone()) {
                    BlockStateCandidate nextCandidate = group.findNextCandidate();
                    if (nextCandidate != null) {
                        nextCandidate.setUsed();
                        this.cachedBlockStates.put(pair.left(), nextCandidate.blockState());
                        this.dirty = true;
                        pair.right().complete(nextCandidate.blockState());
                    } else {
                        pair.right().completeExceptionally(new StateExhaustedException(group, pair.left()));
                    }
                }
            }
        }
    }

    public static class StateExhaustedException extends RuntimeException {
        private final AutoStateGroup group;
        private final String appearance;

        public StateExhaustedException(AutoStateGroup group, String appearance) {
            this.group = group;
            this.appearance = appearance;
        }

        public String appearance() {
            return this.appearance;
        }

        public AutoStateGroup group() {
            return this.group;
        }

        @Override
        public String getMessage() {
            return toString();
        }

        @Override
        public String toString() {
            return "StateExhaustedException{" +
                    "group=" + group +
                    ", appearance='" + appearance + '\'' +
                    '}';
        }
    }

    /**
     * 从文件加载缓存
     */
    public void loadFromCache() throws IOException {
        if (!Files.exists(this.cacheFilePath)) {
            if (!this.cachedBlockStates.isEmpty()) {
                this.cachedBlockStates.clear();
            }
            return;
        }

        long lastTime = Files.getLastModifiedTime(this.cacheFilePath).toMillis();
        if (lastTime != this.lastModified) {
            this.lastModified = lastTime;
            this.cachedBlockStates.clear();
            JsonElement element = GsonHelper.readJsonFromFile(this.cacheFilePath);
            if (element instanceof JsonObject jsonObject) {
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    if (entry.getValue() instanceof JsonPrimitive primitive) {
                        String id = primitive.getAsString();
                        BlockStateWrapper state = this.factory.apply(id);
                        if (state != null) {
                            this.cachedBlockStates.put(entry.getKey(), state);
                        }
                    }
                }
            }
        }
    }

    /**
     * 保存缓存到文件
     */
    public void saveToCache() throws IOException {
        if (!this.dirty) {
            return;
        }

        this.dirty = false;

        // 创建按ID排序的TreeMap
        Map<BlockStateWrapper, String> sortedById = new TreeMap<>();
        for (Map.Entry<String, BlockStateWrapper> entry : this.cachedBlockStates.entrySet()) {
            sortedById.put(entry.getValue(), entry.getKey());
        }
        // 创建有序的JSON对象
        JsonObject sortedJsonObject = new JsonObject();
        for (Map.Entry<BlockStateWrapper, String> entry : sortedById.entrySet()) {
            sortedJsonObject.addProperty(entry.getValue(), entry.getKey().getAsString());
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
}
