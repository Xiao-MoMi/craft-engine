package net.momirealms.craftengine.core.util;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class CompletableFutures {
    private CompletableFutures() {}

    public static <T extends CompletableFuture<?>> Collector<T, ImmutableList.Builder<T>, CompletableFuture<Void>> collector() {
        return Collector.of(
                ImmutableList.Builder::new,
                ImmutableList.Builder::add,
                (l, r) -> l.addAll(r.build()),
                builder -> allOf(builder.build())
        );
    }

    public static CompletableFuture<Void> allOf(Stream<? extends CompletableFuture<?>> futures) {
        CompletableFuture<?>[] arr = futures.toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(arr);
    }

    public static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> futures) {
        CompletableFuture<?>[] arr = futures.toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(arr);
    }
}