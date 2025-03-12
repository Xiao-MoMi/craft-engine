package net.momirealms.craftengine.core.block.properties;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.sparrow.nbt.IntTag;
import net.momirealms.sparrow.nbt.NumericTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class IntegerProperty extends Property<Integer> {
    public static final Factory FACTORY = new Factory();
    private final IntImmutableList values;
    public final int min;
    public final int max;

    private IntegerProperty(String name, int min, int max, int defaultValue) {
        super(name, Integer.class, defaultValue);
        this.min = min;
        this.max = max;
        this.values = IntImmutableList.toList(IntStream.range(min, max + 1));

        final Integer[] byId = new Integer[max - min + 1];
        for (int i = min; i <= max; ++i) {
            byId[i - min] = i;
        }

        this.setById(byId);
    }

    @Override
    public List<Integer> possibleValues() {
        return this.values;
    }

    @Override
    public Optional<Integer> optional(String valueName) {
        try {
            int i = Integer.parseInt(valueName);
            return i >= this.min && i <= this.max ? Optional.of(i) : Optional.empty();
        } catch (NumberFormatException var3) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Tag> createOptionalTag(String valueName) {
        return optional(valueName).map(IntTag::new);
    }

    @Override
    public Tag pack(Integer value) {
        return new IntTag(value);
    }

    @Override
    public final int idFor(final Integer value) {
        final int ret = value - this.min;
        return ret | ((this.max - ret) >> 31);
    }

    @Override
    public Integer unpack(Tag tag) {
        if (tag instanceof NumericTag numericTag) {
            return values.getInt(idFor(numericTag.getAsInt()));
        }
        throw new IllegalArgumentException("Invalid numeric tag: " + tag);
    }

    @Override
    public String valueName(Integer integer) {
        return integer.toString();
    }

    @Override
    public int indexOf(Integer integer) {
        return integer <= this.max ? integer - this.min : -1;
    }

    public static IntegerProperty create(String name, int min, int max, int defaultValue) {
        return new IntegerProperty(name, min, max, defaultValue);
    }

    public static class Factory implements PropertyFactory {
        @Override
        public Property<?> create(String name, Map<String, Object> arguments) {
            String range = arguments.getOrDefault("range", "1~1").toString();
            String[] split = range.split("~");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            int defaultValue = MiscUtils.getAsInt(arguments.getOrDefault("default", min));
            return IntegerProperty.create(name, min, max,defaultValue);
        }
    }
}