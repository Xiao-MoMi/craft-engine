package net.momirealms.craftengine.core.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public final class BlockStateVariantProvider {
    private final ImmutableSortedMap<String, Property<?>> properties;
    private final ImmutableList<ImmutableBlockState> states;
    private final Holder<BlockDefinition> owner;

    public BlockStateVariantProvider(Holder.Reference<BlockDefinition> owner, Factory<Holder.Reference<BlockDefinition>, ImmutableBlockState> factory, Map<String, Property<?>> propertiesMap) {
        this.owner = owner;
        this.properties = ImmutableSortedMap.copyOf(propertiesMap);

        List<ImmutableBlockState> list = Lists.newArrayList();
        Stream<List<Pair<Property<?>, Comparable<?>>>> stream = Stream.of(Collections.emptyList());
        Map<Map<Property<?>, Comparable<?>>, ImmutableBlockState> map = Maps.newLinkedHashMap();

        for (Property<?> property : this.properties.values()) {
            stream = stream.flatMap(entries -> {
                List<Stream<List<Pair<Property<?>, Comparable<?>>>>> streams = new ArrayList<>();
                for (Comparable<?> value : property.possibleValues()) {
                    List<Pair<Property<?>, Comparable<?>>> newEntries = new ArrayList<>(entries);
                    newEntries.add(Pair.of(property, value));
                    streams.add(Stream.of(newEntries));
                }
                return streams.stream().flatMap(Function.identity());
            });
        }

        stream.forEach((entries) -> {
            Reference2ObjectArrayMap<Property<?>, Comparable<?>> reference2ObjectArrayMap = new Reference2ObjectArrayMap<>(entries.size());
            for (Pair<Property<?>, Comparable<?>> entry : entries) {
                reference2ObjectArrayMap.put(entry.left(), entry.right());
            }
            ImmutableBlockState state = factory.create(owner, this, reference2ObjectArrayMap);
            map.put(reference2ObjectArrayMap, state);
            list.add(state);
        });

        for (ImmutableBlockState state : list) {
            state.createWithMap(map);
        }
        this.states = ImmutableList.copyOf(list);
    }

    public List<ImmutableBlockState> getPossibleStates(CompoundTag nbt) {
        List<ImmutableBlockState> tempStates = new ArrayList<>();
        ImmutableBlockState defaultState = getDefaultState();
        tempStates.add(defaultState);
        for (Property<?> property : defaultState.getProperties()) {
            Tag value = nbt.get(property.name());
            if (value != null) {
                tempStates.replaceAll(immutableBlockState -> ImmutableBlockState.with(immutableBlockState, property, property.unpack(value)));
            } else {
                List<ImmutableBlockState> newStates = new ArrayList<>();
                for (ImmutableBlockState state : tempStates) {
                    for (Object possibleValue : property.possibleValues()) {
                        newStates.add(ImmutableBlockState.with(state, property, possibleValue));
                    }
                }
                tempStates = newStates;
            }
        }
        return tempStates;
    }

    public interface Factory<O, S> {
        S create(O owner, BlockStateVariantProvider variantProvider, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap);
    }

    @NotNull
    public ImmutableBlockState getDefaultState() {
        ImmutableBlockState first = this.states.getFirst();
        for (Property<?> property : this.properties.values()) {
            first = ImmutableBlockState.with(first, property, property.defaultValue());
        }
        return first;
    }

    public ImmutableList<ImmutableBlockState> states() {
        return this.states;
    }

    public Holder<BlockDefinition> owner() {
        return this.owner;
    }

    @NotNull
    public ImmutableSortedMap<String, Property<?>> properties() {
        return this.properties;
    }

    @Nullable
    public Property<?> getProperty(String name) {
        return this.properties.get(name);
    }

    public boolean hasProperty(String name) {
        return this.properties.containsKey(name);
    }
}
