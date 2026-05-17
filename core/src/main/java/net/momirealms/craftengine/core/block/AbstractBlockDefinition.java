package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.loot.Loot;
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

public abstract class AbstractBlockDefinition implements BlockDefinition {
    protected final Key id;
    protected final Holder.Reference<BlockDefinition> holder;
    protected final BlockStateVariantProvider variantProvider;
    protected final ImmutableBlockState defaultState;
    protected final Map<EventTrigger, List<Function<Context>>> events;
    @Nullable
    protected final Loot loot;
    protected BlockBehavior behavior;

    protected AbstractBlockDefinition(
            @NotNull Holder.Reference<BlockDefinition> holder,
            @NotNull BlockStateVariantProvider variantProvider,
            @NotNull Map<EventTrigger, List<Function<Context>>> events,
            @Nullable Loot loot
    ) {
        this.id = holder.key().location();
        this.holder = holder;
        this.loot = loot;
        this.events = events;
        this.variantProvider = variantProvider;
        this.defaultState = this.variantProvider.getDefaultState();
    }

    @Override
    public @Nullable Loot loot() {
        return this.loot;
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
    public boolean hasProperty(String name) {
        return this.variantProvider.hasProperty(name);
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
        return this.behavior.updateStateForPlacement(context, defaultState());
    }
}
