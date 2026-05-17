package net.momirealms.craftengine.core.plugin.context;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public final class ContextHolder {
    public static final ContextHolder IMMUTABLE_EMPTY_HOLDER = ContextHolder.builder().immutable(true).build();

    private final Map<ContextKey<?>, Supplier<Object>> params;
    private final boolean immutable;

    private ContextHolder(Map<ContextKey<?>, Supplier<Object>> params, boolean immutable) {
        this.params = params;
        this.immutable = immutable;
    }

    public static ContextHolder trustedImmutable(Map<ContextKey<?>, Supplier<Object>> params) {
        return new ContextHolder(params, true);
    }

    public static ContextHolder trustedMutable(Map<ContextKey<?>, Supplier<Object>> params) {
        return new ContextHolder(params, false);
    }

    public static ContextHolder immutable(Map<ContextKey<?>, Supplier<Object>> params) {
        return new ContextHolder(ImmutableMap.copyOf(params), true);
    }

    public static ContextHolder mutable(Map<ContextKey<?>, Supplier<Object>> params) {
        return new ContextHolder(new HashMap<>(params), false);
    }

    @NotNull
    public static ContextHolder empty() {
        return ContextHolder.builder().build();
    }

    public static ContextHolder emptyImmutable() {
        return IMMUTABLE_EMPTY_HOLDER;
    }

    public boolean immutable() {
        return this.immutable;
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
    public <T> @Nullable T getOrNull(ContextKey<T> parameter) {
        Supplier<Object> supplier = this.params.get(parameter);
        if (supplier == null) {
            return null;
        }
        return (T) supplier.get();
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

    @ApiStatus.Internal
    public Map<ContextKey<?>, Supplier<Object>> params() {
        return ImmutableMap.copyOf(this.params);
    }

    @ApiStatus.Internal
    public boolean isEmpty() {
        return this.params.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ContextKey<?>, Supplier<Object>> params = new HashMap<>(8);
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
            if (this.immutable) {
                return ContextHolder.immutable(this.params);
            } else {
                return new ContextHolder(this.params, false);
            }
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
