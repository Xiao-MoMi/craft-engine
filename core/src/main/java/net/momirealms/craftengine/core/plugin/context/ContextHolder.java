package net.momirealms.craftengine.core.plugin.context;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public class ContextHolder {
    public static final ContextHolder EMPTY = ContextHolder.builder().immutable(true).build();
    protected final Map<ContextKey<?>, Supplier<Object>> params;

    public ContextHolder(Map<ContextKey<?>, Supplier<Object>> params) {
        this.params = params;
    }

    public boolean has(ContextKey<?> key) {
        return this.params.containsKey(key);
    }

    public <T> ContextHolder withParameter(ContextKey<T> parameter, T value) {
        this.params.put(parameter, SimpleSupplier.of(value));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> ContextHolder withParameter(ContextKey<T> parameter, Supplier<T> value) {
        this.params.put(parameter, (Supplier<Object>) value);
        return this;
    }

    public <T> ContextHolder withOptionalParameter(ContextKey<T> parameter, @Nullable T value) {
        if (value == null) {
            this.params.remove(parameter);
        } else {
            this.params.put(parameter, SimpleSupplier.of(value));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrThrow(ContextKey<T> parameter) {
        Supplier<T> object = (Supplier<T>) this.params.get(parameter);
        if (object == null) {
            throw new NoSuchElementException(parameter.node());
        } else {
            return object.get();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(ContextKey<T> parameter) {
        return (Optional<T>) Optional.ofNullable(this.params.get(parameter)).map(Supplier::get);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getOrDefault(ContextKey<T> parameter, @Nullable T defaultValue) {
        return (T) Optional.ofNullable(this.params.get(parameter)).map(Supplier::get).orElse(defaultValue);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ContextKey<?>, Supplier<Object>> params = new HashMap<>();
        private boolean immutable = false;

        public Builder() {}

        public <T> Builder withParameter(ContextKey<T> parameter, T value) {
            this.params.put(parameter, SimpleSupplier.of(value));
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Builder withParameter(ContextKey<T> parameter, Supplier<T> value) {
            this.params.put(parameter, (Supplier<Object>) value);
            return this;
        }

        public <T> Builder withOptionalParameter(ContextKey<T> parameter, @Nullable T value) {
            if (value == null) {
                this.params.remove(parameter);
            } else {
                this.params.put(parameter, SimpleSupplier.of(value));
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> T getParameterOrThrow(ContextKey<T> parameter) {
            Supplier<T> object = (Supplier<T>) this.params.get(parameter);
            if (object == null) {
                throw new NoSuchElementException(parameter.node());
            } else {
                return object.get();
            }
        }

        public Builder immutable(boolean immutable) {
            this.immutable = immutable;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
            return Optional.ofNullable((Supplier<T>) this.params.get(parameter)).map(Supplier::get);
        }

        public ContextHolder build() {
            return new ContextHolder(this.immutable ? ImmutableMap.copyOf(this.params) : this.params);
        }
    }

    public static class SimpleSupplier<T> implements Supplier<T> {
        private final T object;

        public SimpleSupplier(T object) {
            this.object = object;
        }

        @Override
        public T get() {
            return this.object;
        }

        public static <T> SimpleSupplier<T> of(T object) {
            return new SimpleSupplier<>(object);
        }
    }
}
