package net.momirealms.craftengine.core.block.properties;

import net.momirealms.sparrow.nbt.Tag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Property<T extends Comparable<T>> {
    private final Class<T> clazz;
    private final String name;
    @Nullable
    private Integer hashCode;

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();
    private final int id;
    private final T defaultValue;
    private T[] byId;
    private int defaultValueIndex;

    protected Property(String name, Class<T> clazz, T defaultValue) {
        this.clazz = clazz;
        this.name = name;
        this.id = ID_GENERATOR.getAndIncrement();
        this.defaultValue = defaultValue;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public int defaultValueIndex() {
        return defaultValueIndex;
    }

    public int id() {
        return this.id;
    }

    public T byId(int id) {
        final T[] byId = this.byId;
        return id < 0 || id >= byId.length ? null : this.byId[id];
    }

    protected void setById(T[] byId) {
        if (this.byId != null) {
            throw new IllegalStateException();
        }
        this.byId = byId;
        this.defaultValueIndex = indexOf(defaultValue);
    }

    public Value<T> value(T value) {
        return new Value<>(this, value);
    }

    public abstract int idFor(T value);

    public abstract String valueName(T value);

    public abstract List<T> possibleValues();

    public abstract Optional<T> optional(String valueName);

    public abstract Optional<Tag> createOptionalTag(String valueName);

    public abstract Tag pack(T value);

    public int indexOf(Tag tag) {
        return indexOf(unpack(tag));
    }

    public abstract int indexOf(T value);

    public abstract T unpack(Tag tag);

    public String name() {
        return this.name;
    }

    public Class<T> valueClass() {
        return this.clazz;
    }

    @Override
    public boolean equals(Object object) {
        return this == object;
    }

    @Override
    public final int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = this.generateHashCode();
        }
        return this.hashCode;
    }

    protected int generateHashCode() {
        return 31 * this.clazz.hashCode() + this.name.hashCode();
    }

    public record Value<T extends Comparable<T>>(Property<T> property, T value) {
        public Value(Property<T> property, T value) {
            if (!property.possibleValues().contains(value)) {
                throw new IllegalArgumentException("Value " + value + " does not belong to property " + property);
            } else {
                this.property = property;
                this.value = value;
            }
        }

        @Override
        public String toString() {
            return this.property.name + "=" + this.property.valueName(this.value);
        }
    }
}
