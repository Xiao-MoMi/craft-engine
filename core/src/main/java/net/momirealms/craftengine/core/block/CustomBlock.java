package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class CustomBlock {
    protected final Holder<CustomBlock> holder;
    protected Key id;
    protected ImmutableBlockState defaultState;
    protected BlockStateVariantProvider variantProvider;
    protected Map<String, Property<?>> properties;
    protected BlockBehavior behavior;
    @Nullable
    protected LootTable<?> lootTable;

    public CustomBlock(
            @NotNull Key id,
            @NotNull Holder.Reference<CustomBlock> holder,
            @NotNull Map<String, Property<?>> properties,
            @NotNull Map<String, Integer> appearances,
            @NotNull Map<String, VariantState> variantMapper,
            @NotNull BlockSettings settings,
            @Nullable Map<String, Object> behaviorSettings,
            @Nullable LootTable<?> lootTable
    ) {
        holder.bindValue(this);
        this.holder = holder;
        this.id = id;
        this.lootTable = lootTable;
        this.properties = properties;
        this.variantProvider = new BlockStateVariantProvider(holder, ImmutableBlockState::new, properties);
        this.setDefaultState(this.variantProvider.getDefaultState());
        this.behavior = BlockBehaviors.fromMap(this, behaviorSettings);
        for (Map.Entry<String, VariantState> entry : variantMapper.entrySet()) {
            String nbtString = entry.getKey();
            CompoundTag tag = BlockNbtParser.deserialize(this, nbtString);
            if (tag == null) {
                CraftEngine.instance().logger().warn("Illegal block state: " + nbtString);
                continue;
            }
            VariantState variantState = entry.getValue();
            int vanillaStateRegistryId = appearances.getOrDefault(variantState.appearance(), -1);
            if (vanillaStateRegistryId == -1) {
                CraftEngine.instance().logger().warn("Could not find appearance " + variantState.appearance() + " for block " + id);
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
    }

    protected abstract void applyPlatformSettings();

    @Nullable
    public LootTable<?> lootTable() {
        return lootTable;
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
        tempStates.add(getDefaultState());
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
        ImmutableBlockState state = getDefaultState();
        for (Map.Entry<String, Tag> entry : nbt.tags.entrySet()) {
            Property<?> property = variantProvider.getProperty(entry.getKey());
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

    protected final void setDefaultState(ImmutableBlockState state) {
        this.defaultState = state;
    }

    public final ImmutableBlockState getDefaultState() {
        return this.defaultState;
    }

    // TODO refactor this method
    @SuppressWarnings("unchecked")
    public ImmutableBlockState getStateForPlacement(BlockPlaceContext context) {
        ImmutableBlockState state = getDefaultState();
        Property<Direction.Axis> axisProperty = (Property<Direction.Axis>) this.variantProvider.getProperty("axis");
        if (axisProperty != null) {
            state = state.with(axisProperty, context.getClickedFace().axis());
        }
        Property<?> directionProperty = this.variantProvider.getProperty("facing");
        if (directionProperty != null) {
            if (directionProperty.valueClass() == HorizontalDirection.class) {
                state = state.with((Property<HorizontalDirection>) directionProperty, context.getHorizontalDirection().opposite().toHorizontalDirection());
            } else if (directionProperty.valueClass() == Direction.class) {
                state = state.with((Property<Direction>) directionProperty, context.getNearestLookingDirection().opposite());
            }
        }
        Property<Boolean> waterloggedProperty = (Property<Boolean>) this.variantProvider.getProperty("waterlogged");
        if (waterloggedProperty != null) {
            if (context.isWaterSource()) {
                state = state.with(waterloggedProperty, true);
            }
        }

        //TODO Use default values
        Property<Boolean> persistentProperty = (Property<Boolean>) this.variantProvider.getProperty("persistent");
        if (persistentProperty != null) {
            state = state.with(persistentProperty, true);
        }
        Property<Integer> distanceProperty = (Property<Integer>) this.variantProvider.getProperty("distance");
        if (distanceProperty != null) {
            state = state.with(distanceProperty, distanceProperty.defaultValue());
        }
        //TODO state = state.behavior().getStateForPlacement(state, context);
        return state;
    }
}
