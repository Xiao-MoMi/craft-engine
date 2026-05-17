package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.Nullable;

public final class ExceptionCollector<T extends Throwable> {
    private final Class<T> exceptionClass;
    @Nullable
    private T result;

    public ExceptionCollector(Class<T> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public void add(T throwable) {
        if (this.result == null) {
            this.result = throwable;
        } else {
            this.result.addSuppressed(throwable);
        }
    }

    public @Nullable T result() {
        return result;
    }

    public void throwIfPresent() throws T {
        if (this.result != null) {
            throw this.result;
        }
    }

    public void addAndThrow(T throwable) throws T {
        this.add(throwable);
        this.throwIfPresent();
    }

    public void runCatching(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (this.exceptionClass.isInstance(t)) {
                this.add(this.exceptionClass.cast(t));
            } else {
                ThrowableUtils.sneakyThrow(t);
            }
        }
    }
}

