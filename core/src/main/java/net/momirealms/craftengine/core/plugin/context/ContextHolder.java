package net.momirealms.craftengine.core.plugin.context;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ContextHolder {
    public static final ContextHolder IMMUTABLE_EMPTY_HOLDER = ContextHolder.builder().immutable(true).build();
    private static final Object ABSENT = new Object();

    private final Map<ContextKey<?>, Object> params;
    private final boolean immutable;

    private ContextHolder(Map<ContextKey<?>, Object> params, boolean immutable) {
        this.params = params;
        this.immutable = immutable;
    }

    public static ContextHolder trustedImmutable(Map<ContextKey<?>, Object> params) {
        return new ContextHolder(params, true);
    }

    public static ContextHolder trustedMutable(Map<ContextKey<?>, Object> params) {
        return new ContextHolder(params, false);
    }

    public static ContextHolder immutable(Map<ContextKey<?>, Object> params) {
        return new ContextHolder(Collections.unmodifiableMap(params), true);
    }

    public static ContextHolder mutable(Map<ContextKey<?>, Object> params) {
        return new ContextHolder(new Object2ObjectArrayMap<>(params), false);
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

    public ContextHolder copy() {
        if (this.immutable) {
            return new ContextHolder(Collections.unmodifiableMap(new Object2ObjectOpenHashMap<>(this.params)), true);
        } else {
            return new ContextHolder(new Object2ObjectOpenHashMap<>(this.params), false);
        }
    }

    public <T> ContextHolder withParameter(ContextKey<T> parameter, T value) {
        this.params.put(parameter, value);
        return this;
    }

    public <T> ContextHolder withOptionalParameter(ContextKey<T> parameter, @Nullable T value) {
        if (value == null) {
            this.params.remove(parameter);
        } else {
            this.params.put(parameter, value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getOrNull(ContextKey<T> parameter) {
        return (T) this.params.get(parameter);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrThrow(ContextKey<T> parameter) {
        Object value = this.params.getOrDefault(parameter, ABSENT);
        if (value == ABSENT) {
            throw new NoSuchElementException(parameter.node());
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(ContextKey<T> parameter) {
        return Optional.ofNullable((T) this.params.get(parameter));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getOrDefault(ContextKey<T> parameter, @Nullable T defaultValue) {
        T value = (T) this.params.get(parameter);
        return value != null ? value : defaultValue;
    }

    @ApiStatus.Internal
    public Map<ContextKey<?>, Object> params() {
        return ImmutableMap.copyOf(this.params);
    }

    @ApiStatus.Internal
    public boolean isEmpty() {
        return this.params.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ContextKey<?> k1, Object v1) {
        return new Builder(new ContextKey<?>[]{k1}, new Object[]{v1});
    }

    public static Builder builder(ContextKey<?> k1, Object v1,
                                  ContextKey<?> k2, Object v2) {
        return new Builder(new ContextKey<?>[]{k1, k2}, new Object[]{v1, v2});
    }

    public static Builder builder(ContextKey<?> k1, Object v1,
                                  ContextKey<?> k2, Object v2,
                                  ContextKey<?> k3, Object v3) {
        return new Builder(new ContextKey<?>[]{k1, k2, k3}, new Object[]{v1, v2, v3});
    }

    public static Builder builder(ContextKey<?> k1, Object v1,
                                  ContextKey<?> k2, Object v2,
                                  ContextKey<?> k3, Object v3,
                                  ContextKey<?> k4, Object v4) {
        return new Builder(new ContextKey<?>[]{k1, k2, k3, k4}, new Object[]{v1, v2, v3, v4});
    }

    public static class Builder {
        private final Map<ContextKey<?>, Object> params;
        private boolean immutable = false;

        Builder(ContextKey<?>[] keys, Object[] values) {
            this.params = new Object2ObjectArrayMap<>(keys, values);
        }

        Builder() {
            this.params = new Object2ObjectOpenHashMap<>();
        }

        public <T> Builder withParameter(ContextKey<T> parameter, T value) {
            this.params.put(parameter, value);
            return this;
        }

        public <T> Builder withOptionalParameter(ContextKey<T> parameter, @Nullable T value) {
            if (value == null) {
                this.params.remove(parameter);
            } else {
                this.params.put(parameter, value);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> T getParameterOrThrow(ContextKey<T> parameter) {
            Object value = this.params.getOrDefault(parameter, ABSENT);
            if (value == ABSENT) {
                throw new NoSuchElementException(parameter.node());
            }
            return (T) value;
        }

        public Builder immutable(boolean immutable) {
            this.immutable = immutable;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
            return Optional.ofNullable((T) this.params.get(parameter));
        }

        public ContextHolder build() {
            if (this.immutable) {
                return ContextHolder.immutable(this.params);
            } else {
                return new ContextHolder(this.params, false);
            }
        }
    }
}
