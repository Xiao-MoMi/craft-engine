package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public final class ThrowableUtils {
    private ThrowableUtils() {}

    @NotNull
    public static <T extends Throwable> T combine(@Nullable T first, @NotNull T next) {
        if (first == null) return next;
        first.addSuppressed(next);
        return first;
    }

    @NotNull
    public static Throwable unwrapCompletion(@NotNull Throwable throwable) {
        return throwable instanceof CompletionException completionException && completionException.getCause() != null
                ? completionException.getCause()
                : throwable;
    }

    public static <T> T sneakyThrow(ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> E sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    @FunctionalInterface
    public interface ThrowableSupplier<T> {

        T get() throws Throwable;
    }
}
