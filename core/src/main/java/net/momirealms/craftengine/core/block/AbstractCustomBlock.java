package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public abstract class AbstractCustomBlock implements CustomBlock {
    protected final Key id;
    protected final Holder.Reference<CustomBlock> holder;
    protected final BlockStateVariantProvider variantProvider;
    protected final BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState> placementFunction;
    protected final ImmutableBlockState defaultState;
    protected final Map<EventTrigger, List<Function<Context>>> events;
    @Nullable
    protected final LootTable<?> lootTable;
    protected BlockBehavior behavior;

    protected AbstractCustomBlock(
            @NotNull Holder.Reference<CustomBlock> holder,
            @NotNull BlockStateVariantProvider variantProvider,
            @NotNull Map<EventTrigger, List<Function<Context>>> events,
            @Nullable LootTable<?> lootTable
    ) {
        this.id = holder.key().location();
        this.holder = holder;
        this.lootTable = lootTable;
        this.events = events;
        this.variantProvider = variantProvider;
        this.defaultState = this.variantProvider.getDefaultState();
        List<BiFunction<BlockPlaceContext, ImmutableBlockState, ImmutableBlockState>> placements = new ArrayList<>(4);
        for (Map.Entry<String, Property<?>> propertyEntry : this.variantProvider.properties().entrySet()) {
            placements.add(Property.createStateForPlacement(propertyEntry.getKey(), propertyEntry.getValue()));
        }
        this.placementFunction = composite(placements);
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

    @Override
    public @Nullable LootTable<?> lootTable() {
        return this.lootTable;
    }

    @Override
    public void execute(Context context, EventTrigger trigger) {
        for (Function<Context> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
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

    public void setBehavior(@Nullable BlockBehavior behavior) {
        this.behavior = behavior;
    }

    @Override
    public List<ImmutableBlockState> getPossibleStates(CompoundTag nbt) {
        return this.variantProvider.getPossibleStates(nbt);
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
        return this.variantProvider.getProperty(name);
    }

    @Override
    public @NotNull Collection<Property<?>> properties() {
        return this.variantProvider.properties().values();
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
    }
}
