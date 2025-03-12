package net.momirealms.craftengine.core.util.context;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ContextHolder {
    public static final ContextHolder EMPTY = ContextHolder.builder().build();

    private final Map<ContextKey<?>, Object> params;

    public ContextHolder(Map<ContextKey<?>, Object> params) {
        this.params = params;
    }

    public boolean has(ContextKey<?> key) {
        return params.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrThrow(ContextKey<T> parameter) {
        T object = (T) this.params.get(parameter);
        if (object == null) {
            throw new NoSuchElementException(parameter.id().toString());
        } else {
            return object;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(ContextKey<T> parameter) {
        return Optional.ofNullable((T) this.params.get(parameter));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Contract("_,!null->!null; _,_->_")
    public <T> T getOrDefault(ContextKey<T> parameter, @Nullable T defaultValue) {
        return (T) this.params.getOrDefault(parameter, defaultValue);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<ContextKey<?>, Object> params = new HashMap<>();

        public Builder() {}

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
            T object = (T) this.params.get(parameter);
            if (object == null) {
                throw new NoSuchElementException(parameter.id().toString());
            } else {
                return object;
            }
        }

        @SuppressWarnings("unchecked")
        public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
            return Optional.ofNullable((T) this.params.get(parameter));
        }

        public ContextHolder build() {
            return new ContextHolder(this.params);
        }
    }
}
