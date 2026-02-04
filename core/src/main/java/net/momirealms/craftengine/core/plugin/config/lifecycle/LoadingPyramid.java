package net.momirealms.craftengine.core.plugin.config.lifecycle;

import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LoadingPyramid {
    private final Map<LoadingStage, ConfigTask> tasks = new ConcurrentHashMap<>();
    private final Map<LoadingStage, CompletableFuture<Void>> futures = new ConcurrentHashMap<>();

    private static class ConfigTask {
        LoadingStage stage;
        List<LoadingStage> dependencies;
        Runnable action;

        ConfigTask(LoadingStage stage, List<LoadingStage> dependencies, Runnable action) {
            this.stage = stage;
            this.dependencies = dependencies;
            this.action = action;
        }
    }

    public void addTask(LoadingStage stage, List<LoadingStage> dependencies, Runnable action) {
        this.tasks.put(stage, new ConfigTask(stage, dependencies, action));
    }

    public CompletableFuture<Void> execute() {
        checkCyclicDependencies();
        for (LoadingStage stage : this.tasks.keySet()) {
            getOrCreateFuture(stage);
        }
        return CompletableFuture.allOf(this.futures.values().toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> getOrCreateFuture(LoadingStage stage) {
        if (this.futures.containsKey(stage)) {
            return this.futures.get(stage);
        }

        ConfigTask task = this.tasks.get(stage);
        if (task == null) {
            throw new IllegalStateException("Undefined stage: " + stage);
        }

        CompletableFuture<Void> future;
        if (task.dependencies.isEmpty()) {
            // 无依赖，直接异步开启
            future = CompletableFuture.runAsync(task.action, CraftEngine.instance().scheduler().async());
        } else {
            // 有依赖，等待所有前置任务完成后触发
            CompletableFuture<?>[] depFutures = task.dependencies.stream()
                    .map(this::getOrCreateFuture)
                    .toArray(CompletableFuture[]::new);

            future = CompletableFuture.allOf(depFutures)
                    .thenRunAsync(task.action, CraftEngine.instance().scheduler().async());
        }

        this.futures.put(stage, future);
        return future;
    }

    private void checkCyclicDependencies() {
        Set<LoadingStage> visiting = new HashSet<>();
        Set<LoadingStage> visited = new HashSet<>();
        for (LoadingStage node : this.tasks.keySet()) {
            if (hasCycle(node, visiting, visited)) {
                throw new IllegalStateException("Cycle detected: " + node);
            }
        }
    }

    private boolean hasCycle(LoadingStage node, Set<LoadingStage> visiting, Set<LoadingStage> visited) {
        if (visiting.contains(node)) return true;
        if (visited.contains(node)) return false;

        visiting.add(node);
        ConfigTask task = this.tasks.get(node);
        if (task != null) {
            for (LoadingStage dep : task.dependencies) {
                if (hasCycle(dep, visiting, visited)) return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }
}