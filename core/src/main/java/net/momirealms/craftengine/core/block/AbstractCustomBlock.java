package net.momirealms.craftengine.core.block;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.core.block.parser.BlockNbtParser;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public abstract class AbstractCustomBlock implements CustomBlock {
    protected final Holder<CustomBlock> holder;
    protected final Key id;
    protected final BlockStateVariantProvider variantProvider;
    protected final Map<String, Property<?>> properties;
    protected final BlockBehavior behavior;
    protected final BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> placementFunction;
    protected final ImmutableBlockState defaultState;
    protected final Map<EventTrigger, List<Function<PlayerOptionalContext>>> events;
    @Nullable
    protected final LootTable<?> lootTable;

    protected AbstractCustomBlock(
            @NotNull Key id,
            @NotNull Holder.Reference<CustomBlock> holder,
            @NotNull Map<String, Property<?>> properties,
            @NotNull Map<String, Integer> appearances,
            @NotNull Map<String, BlockStateVariant> variantMapper,
            @NotNull BlockSettings settings,
            @NotNull Map<EventTrigger, List<Function<PlayerOptionalContext>>> events,
            @Nullable List<Map<String, Object>> behaviorConfig,
            @Nullable LootTable<?> lootTable
    ) {
        holder.bindValue(this);
        this.holder = holder;
        this.id = id;
        this.lootTable = lootTable;
        this.properties = ImmutableMap.copyOf(properties);
        this.events = events;
        this.variantProvider = new BlockStateVariantProvider(holder, ImmutableBlockState::new, properties);
        this.defaultState = this.variantProvider.getDefaultState();
        this.behavior = setupBehavior(behaviorConfig);
        List<BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState>> placements = new ArrayList<>(4);
        for (Map.Entry<String, Property<?>> propertyEntry : this.properties.entrySet()) {
            placements.add(Property.createStateForPlacement(propertyEntry.getKey(), propertyEntry.getValue()));
        }
        this.placementFunction = composite(placements);
        for (Map.Entry<String, BlockStateVariant> entry : variantMapper.entrySet()) {
            String nbtString = entry.getKey();
            CompoundTag tag = BlockNbtParser.deserialize(this, nbtString);
            if (tag == null) {
                throw new LocalizedResourceConfigException("warning.config.block.state.property.invalid_format", nbtString);
            }
            BlockStateVariant blockStateVariant = entry.getValue();
            int vanillaStateRegistryId = appearances.getOrDefault(blockStateVariant.appearance(), -1);
            // This should never happen
            if (vanillaStateRegistryId == -1) {
                vanillaStateRegistryId = appearances.values().iterator().next();
            }
            // Late init states
            for (ImmutableBlockState state : this.getPossibleStates(tag)) {
                state.setBehavior(this.behavior);
                state.setSettings(blockStateVariant.settings());
                state.setVanillaBlockState((BlockStateWrapper.VanillaBlockState) BlockRegistryMirror.stateByRegistryId(vanillaStateRegistryId));
                state.setCustomBlockState((BlockStateWrapper.CustomBlockState) BlockRegistryMirror.stateByRegistryId(blockStateVariant.internalRegistryId()));
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

    protected BlockBehavior setupBehavior(List<Map<String, Object>> behaviorConfig) {
        return EmptyBlockBehavior.INSTANCE;
    }

    private static BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> composite(List<BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState>> placements) {
        return switch (placements.size()) {
            case 0 -> (c, i) -> i;
            case 1 -> placements.get(0);
            case 2 -> {
                BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> f1 = placements.get(0);
                BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> f2 = placements.get(1);
                yield (c, i) -> f2.apply(c, f1.apply(c, i));
            }
            default -> (c, i) -> {
                for (BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> f : placements) {
                    i = f.apply(c, i);
                }
                return i;
            };
        };
    }

    protected abstract void applyPlatformSettings();

    @Override
    public @Nullable LootTable<?> lootTable() {
        return this.lootTable;
    }

    @Override
    public void execute(PlayerOptionalContext context, EventTrigger trigger) {
        for (Function<PlayerOptionalContext> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
    }

    @NotNull
    @Override
    public BlockStateVariantProvider variantProvider() {
        return this.variantProvider;
    }

    @NotNull
    @Override
    public final Key id() {
        return this.id;
    }

    @Override
    public List<ImmutableBlockState> getPossibleStates(CompoundTag nbt) {
        List<ImmutableBlockState> tempStates = new ArrayList<>();
        tempStates.add(defaultState());
        for (Property<?> property : this.variantProvider.getDefaultState().getProperties()) {
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

    @Override
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

    @Override
    public @Nullable Property<?> getProperty(String name) {
        return this.properties.get(name);
    }

    @Override
    public @NotNull Collection<Property<?>> properties() {
        return this.properties.values();
    }

    @Override
    public final ImmutableBlockState defaultState() {
        return this.defaultState;
    }

    @Override
    public ImmutableBlockState getStateForPlacement(BlockPlaceContext context) {
        ImmutableBlockState state = this.placementFunction.apply(context, defaultState());
        return this.behavior.updateStateForPlacement(context, state);
    }

    @Override
    public void setPlacedBy(BlockPlaceContext context, ImmutableBlockState state) {
        this.behavior.setPlacedBy(context, state);
    }
}
