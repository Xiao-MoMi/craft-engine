package net.momirealms.craftengine.core.registry;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Holder<T> {

    T value();

    boolean isBound();

    boolean matchesKey(Key id);

    boolean matchesKey(ResourceKey<T> key);

    boolean matchesPredicate(Predicate<ResourceKey<T>> predicate);

    boolean hasTag(ResourceKey<T> tag);

    Stream<ResourceKey<T>> tags();

    Optional<ResourceKey<T>> keyOptional();

    HolderKind kind();

    boolean serializableIn(Owner<T> owner);

    default String registeredName() {
        return this.keyOptional().map(key -> key.location().toString()).orElse("[unregistered]");
    }

    static <T> Holder<T> direct(T value) {
        return new Direct<>(value);
    }

    record Direct<T>(T value) implements Holder<T> {
        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean matchesKey(Key id) {
            return false;
        }

        @Override
        public boolean matchesKey(ResourceKey<T> key) {
            return false;
        }

        @Override
        public boolean hasTag(ResourceKey<T> tag) {
            return false;
        }

        @Override
        public boolean matchesPredicate(Predicate<ResourceKey<T>> predicate) {
            return false;
        }

        @Override
        public Optional<ResourceKey<T>> keyOptional() {
            return Optional.empty();
        }

        @Override
        public HolderKind kind() {
            return HolderKind.DIRECT;
        }

        @Override
        public boolean serializableIn(Owner<T> owner) {
            return true;
        }

        @Override
        public Stream<ResourceKey<T>> tags() {
            return Stream.of();
        }

        @Override
        public String toString() {
            return "Direct{" + this.value + "}";
        }
    }

    enum HolderKind {
        REFERENCE,
        DIRECT
    }

    class Reference<T> implements Holder<T> {
        private final Owner<T> owner;
        @Nullable
        private ResourceKey<T> key;
        @Nullable
        private T value;
        @Nullable
        private Set<ResourceKey<T>> tags;

        public Reference(Owner<T> owner, @Nullable ResourceKey<T> key, @Nullable T value) {
            this.owner = owner;
            this.key = key;
            this.value = value;
        }

        public static <T> Reference<T> create(Owner<T> owner, ResourceKey<T> registryKey) {
            return new Reference<>(owner, registryKey, null);
        }

        public ResourceKey<T> key() {
            if (this.key == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.value + "' from registry " + this.owner);
            }
            return this.key;
        }

        @Override
        public T value() {
            if (this.value == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.key + "' from registry " + this.owner);
            }
            return this.value;
        }

        @Override
        public boolean matchesKey(Key id) {
            return this.key().location().equals(id);
        }

        @Override
        public boolean matchesKey(ResourceKey<T> key) {
            return this.key() == key;
        }

        private Set<ResourceKey<T>> boundTags() {
            if (this.tags == null) {
                throw new IllegalStateException("Tags not bound");
            }
            return this.tags;
        }

        @Override
        public boolean hasTag(ResourceKey<T> tag) {
            return this.boundTags().contains(tag);
        }

        @Override
        public boolean matchesPredicate(Predicate<ResourceKey<T>> predicate) {
            return predicate.test(this.key());
        }

        @Override
        public boolean serializableIn(Owner<T> owner) {
            return this.owner.canSerializeIn(owner);
        }

        @Override
        public Optional<ResourceKey<T>> keyOptional() {
            return Optional.of(this.key());
        }

        @Override
        public HolderKind kind() {
            return HolderKind.REFERENCE;
        }

        @Override
        public boolean isBound() {
            return this.key != null && this.value != null;
        }

        public void bindKey(ResourceKey<T> registryKey) {
            if (this.key != null && registryKey != this.key) {
                throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + registryKey);
            }
            this.key = registryKey;
        }

        public void bindValue(T value) {
            this.value = value;
        }

        public void bindTags(Collection<ResourceKey<T>> tags) {
            this.tags = Collections.unmodifiableSet(new ReferenceOpenHashSet<>(tags));
        }

        @Override
        public Stream<ResourceKey<T>> tags() {
            return this.boundTags().stream();
        }

        @Override
        public String toString() {
            return "Reference{" + this.key + "=" + this.value + "}";
        }
    }

    interface Owner<T> {
        default boolean canSerializeIn(Owner<T> other) {
            return other == this;
        }
    }
}
