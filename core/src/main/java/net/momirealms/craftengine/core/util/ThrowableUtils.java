package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ThrowableUtils {
    private ThrowableUtils() {}

    /**
     * 将后续异常合并到首个异常中, 保留首个异常作为最终抛出值.
     *
     * @param first 已收集的首个异常, 尚未收集时为 {@code null}
     * @param next 后续异常
     * @param <T> 异常类型
     * @return 首个异常或首次传入的异常
     */
    @NotNull
    public static <T extends Throwable> T combine(@Nullable T first, @NotNull T next) {
        if (first == null) return next;
        first.addSuppressed(next);
        return first;
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
