package net.momirealms.craftengine.core.util;

public final class ThrowableUtils {
    private ThrowableUtils() {}

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
