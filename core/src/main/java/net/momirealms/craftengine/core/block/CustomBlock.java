package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public abstract class CustomBlock {
    protected final Holder<CustomBlock> holder;
    protected final Key id;
    protected final BlockStateVariantProvider variantProvider;
    protected final Map<String, Property<?>> properties;
    protected final BlockBehavior behavior;
    protected final List<BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState>> placements;
    protected final ImmutableBlockState defaultState;
    protected final EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events;
    @Nullable
    protected final LootTable<?> lootTable;

    protected CustomBlock(
            @NotNull Key id,
            @NotNull Holder.Reference<CustomBlock> holder,
            @NotNull Map<String, Property<?>> properties,
            @NotNull Map<String, Integer> appearances,
            @NotNull Map<String, VariantState> variantMapper,
            @NotNull BlockSettings settings,
            @NotNull EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events,
            @Nullable Map<String, Object> behavior,
            @Nullable LootTable<?> lootTable
    ) {
        holder.bindValue(this);
        this.holder = holder;
        this.id = id;
        this.lootTable = lootTable;
        this.properties = properties;
        this.placements = new ArrayList<>();
        this.events = events;
        this.variantProvider = new BlockStateVariantProvider(holder, ImmutableBlockState::new, properties);
        this.defaultState = this.variantProvider.getDefaultState();
        this.behavior = BlockBehaviors.fromMap(this, behavior);
        for (Map.Entry<String, VariantState> entry : variantMapper.entrySet()) {
            String nbtString = entry.getKey();
            CompoundTag tag = BlockNbtParser.deserialize(this, nbtString);
            if (tag == null) {
                throw new LocalizedResourceConfigException("warning.config.block.state.property.invalid_format", nbtString);
            }
            VariantState variantState = entry.getValue();
            int vanillaStateRegistryId = appearances.getOrDefault(variantState.appearance(), -1);
            // This should never happen
            if (vanillaStateRegistryId == -1) {
                vanillaStateRegistryId = appearances.values().iterator().next();
            }
            // Late init states
            for (ImmutableBlockState state : this.getPossibleStates(tag)) {
                state.setBehavior(this.behavior);
                state.setSettings(variantState.settings());
                state.setVanillaBlockState(BlockRegistryMirror.stateByRegistryId(vanillaStateRegistryId));
                state.setCustomBlockState(BlockRegistryMirror.stateByRegistryId(variantState.internalRegistryId()));
            }
        }
        // double check if there's any invalid state
        for (ImmutableBlockState state : this.variantProvider().states()) {
            if (state.settings() == null) {
                state.setSettings(settings);
            }
        }
        this.applyPlatformSettings();
        for (Map.Entry<String, Property<?>> propertyEntry : this.properties.entrySet()) {
            this.placements.add(Property.createStateForPlacement(propertyEntry.getKey(), propertyEntry.getValue()));
        }
    }

    protected abstract void applyPlatformSettings();

    @Nullable
    public LootTable<?> lootTable() {
        return lootTable;
    }

    public void execute(PlayerOptionalContext context, EventTrigger trigger) {
        for (Function<PlayerOptionalContext> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
    }

    @NotNull
    public BlockStateVariantProvider variantProvider() {
        return variantProvider;
    }

    @NotNull
    public final Key id() {
        return id;
    }

    private List<ImmutableBlockState> getPossibleStates(CompoundTag nbt) {
        List<ImmutableBlockState> tempStates = new ArrayList<>();
        tempStates.add(defaultState());
        for (Property<?> property : variantProvider.getDefaultState().getProperties()) {
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

    public ImmutableBlockState getBlockState(CompoundTag nbt) {
        ImmutableBlockState state = defaultState();
        for (Map.Entry<String, Tag> entry : nbt.tags.entrySet()) {
            Property<?> property = this.variantProvider.getProperty(entry.getKey());
            if (property != null) {
                try {
                    state = ImmutableBlockState.with(state, property, property.unpack(entry.getValue()));
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Failed to parse block state: " + entry.getKey(), e);
                }
            }
        }
        return state;
    }

    @Nullable
    public Property<?> getProperty(String name) {
        return this.properties.get(name);
    }

    @NotNull
    public Collection<Property<?>> properties() {
        return this.properties.values();
    }

    public final ImmutableBlockState defaultState() {
        return this.defaultState;
    }

    public ImmutableBlockState getStateForPlacement(BlockPlaceContext context) {
        ImmutableBlockState state = defaultState();
        for (BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> placement : this.placements) {
            state = placement.apply(context, state);
        }
        if (this.behavior instanceof AbstractBlockBehavior blockBehavior) {
            state = blockBehavior.updateStateForPlacement(context, state);
        }
        return state;
    }

    public abstract static class Builder {
        protected final Key id;
        protected Map<String, Property<?>> properties;
        protected Map<String, Integer> appearances;
        protected Map<String, VariantState> variantMapper;
        protected BlockSettings settings;
        protected Map<String, Object> behavior;
        protected LootTable<?> lootTable;
        protected EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events;

        protected Builder(Key id) {
            this.id = id;
        }

        public Builder events(EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events) {
            this.events = events;
            return this;
        }

        public Builder appearances(Map<String, Integer> appearances) {
            this.appearances = appearances;
            return this;
        }

        public Builder behavior(Map<String, Object> behavior) {
            this.behavior = behavior;
            return this;
        }

        public Builder lootTable(LootTable<?> lootTable) {
            this.lootTable = lootTable;
            return this;
        }

        public Builder properties(Map<String, Property<?>> properties) {
            this.properties = properties;
            return this;
        }

        public Builder settings(BlockSettings settings) {
            this.settings = settings;
            return this;
        }

        public Builder variantMapper(Map<String, VariantState> variantMapper) {
            this.variantMapper = variantMapper;
            return this;
        }

        public Map<String, Integer> appearances() {
            return appearances;
        }

        public Map<String, Object> behavior() {
            return behavior;
        }

        public Key id() {
            return id;
        }

        public LootTable<?> lootTable() {
            return lootTable;
        }

        public Map<String, Property<?>> properties() {
            return properties;
        }

        public BlockSettings settings() {
            return settings;
        }

        public Map<String, VariantState> variantMapper() {
            return variantMapper;
        }

        public abstract CustomBlock build();
    }
}
