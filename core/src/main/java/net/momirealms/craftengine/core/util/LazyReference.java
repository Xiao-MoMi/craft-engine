package net.momirealms.craftengine.core.util;

import java.util.function.Supplier;

public interface LazyReference<T> {
    
    T get();

    static <T> LazyReference<T> lazyReference(final Supplier<T> supplier) {
        return new LazyReference<>() {
            private T value;

            @Override
            public T get() {
                if (this.value == null) {
                    this.value = supplier.get();
                }
                return this.value;
            }
        };
    }
}
