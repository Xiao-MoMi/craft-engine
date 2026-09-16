package net.momirealms.craftengine.bukkit.plugin.command.feature;

import java.util.List;
import java.util.concurrent.CompletableFuture;

record PackCommandResult(int updated, int unchanged, int failed) {
    static CompletableFuture<PackCommandResult> collect(List<CompletableFuture<Boolean>> operations) {
        List<CompletableFuture<Integer>> results = operations.stream()
                .map(operation -> operation.handle((changed, error) -> error != null ? -1 : (changed ? 1 : 0)))
                .toList();
        return CompletableFuture.allOf(results.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            int updated = 0, unchanged = 0, failed = 0;
            for (CompletableFuture<Integer> result : results) {
                switch (result.join()) {
                    case 1 -> updated++;
                    case 0 -> unchanged++;
                    default -> failed++;
                }
            }
            return new PackCommandResult(updated, unchanged, failed);
        });
    }
}
