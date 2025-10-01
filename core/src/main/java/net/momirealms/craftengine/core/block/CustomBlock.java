package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface CustomBlock {

    Key id();

    @Nullable
    LootTable<?> lootTable();

    void execute(PlayerOptionalContext context, EventTrigger trigger);

    @NotNull
    BlockStateVariantProvider variantProvider();

    List<ImmutableBlockState> getPossibleStates(CompoundTag nbt);

    ImmutableBlockState getBlockState(CompoundTag nbt);

    @Nullable Property<?> getProperty(String name);

    @NotNull Collection<Property<?>> properties();

    ImmutableBlockState defaultState();

    ImmutableBlockState getStateForPlacement(BlockPlaceContext context);

    void setPlacedBy(BlockPlaceContext context, ImmutableBlockState state);
}
