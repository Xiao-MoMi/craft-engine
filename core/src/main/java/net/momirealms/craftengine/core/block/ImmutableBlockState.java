package net.momirealms.craftengine.core.block;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityType;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class ImmutableBlockState {
    private final Holder.Reference<CustomBlock> owner;
    private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap;
    private Map<Property<?>, ImmutableBlockState[]> withMap;

    private CompoundTag tag;
    private BlockStateWrapper customBlockState;
    private BlockStateWrapper visualBlockState;
    // 安全的，在卸载ce后还原的方块
    private BlockStateWrapper restoreBlockState;
    private BlockBehavior behavior;
    private BlockSettings settings;
    private BlockEntityType<? extends BlockEntity> blockEntityType;
    @Nullable
    private BlockEntityElementConfig<? extends BlockEntityElement>[] renderers;
    @Nullable
    private CullingData cullingData;

    ImmutableBlockState(
            Holder.Reference<CustomBlock> owner,
            Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap
    ) {
        this.owner = owner;
        this.propertyMap = new Reference2ObjectArrayMap<>(propertyMap);
    }

    public BlockBehavior behavior() {
        return this.behavior;
    }

    public void setBehavior(BlockBehavior behavior) {
        this.behavior = behavior;
    }

    public BlockSettings settings() {
        return this.settings;
    }

    public void setSettings(BlockSettings settings) {
        this.settings = settings;
    }

    public BlockEntityType<? extends BlockEntity> blockEntityType() {
        return blockEntityType;
    }

    public void setBlockEntityType(BlockEntityType<? extends BlockEntity> blockEntityType) {
        this.blockEntityType = blockEntityType;
    }

    public boolean isEmpty() {
        return this == EmptyBlock.STATE;
    }

    public BlockEntityElementConfig<? extends BlockEntityElement>[] constantRenderers() {
        return this.renderers;
    }

    public void setConstantRenderers(BlockEntityElementConfig<? extends BlockEntityElement>[] renderers) {
        this.renderers = renderers;
    }

    @Nullable
    public CullingData cullingData() {
        return cullingData;
    }

    public void setCullingData(@Nullable CullingData cullingData) {
        this.cullingData = cullingData;
    }

    public boolean hasBlockEntity() {
        return this.blockEntityType != null;
    }

    public boolean hasConstantBlockEntityRenderer() {
        return this.renderers != null;
    }

    public BlockStateWrapper customBlockState() {
        return this.customBlockState;
    }

    @Deprecated
    public BlockStateWrapper vanillaBlockState() {
        return this.visualBlockState;
    }

    public BlockStateWrapper visualBlockState() {
        return this.visualBlockState;
    }

    @ApiStatus.Internal
    public BlockStateWrapper restoreBlockState() {
        return this.restoreBlockState;
    }

    public void setCustomBlockState(@NotNull BlockStateWrapper customBlockState) {
        this.customBlockState = customBlockState;
    }

    public void setVisualBlockState(@NotNull BlockStateWrapper visualBlockState) {
        this.visualBlockState = visualBlockState;
    }

    public void setRestoreBlockState(BlockStateWrapper restoreBlockState) {
        this.restoreBlockState = restoreBlockState;
    }

    public CompoundTag getNbtToSave() {
        if (this.tag == null) {
            this.tag = toNbtToSave(propertiesNbt());
        }
        return this.tag;
    }

    public CompoundTag toNbtToSave(CompoundTag properties) {
        CompoundTag tag = new CompoundTag();
        tag.put("properties", properties);
        tag.put("id", NBT.createString(this.owner.key().location().asString()));
        return tag;
    }

    public void setNbtToSave(CompoundTag tag) {
        this.tag = tag;
    }

    @SuppressWarnings("unchecked")
    public List<Item<Object>> getDrops(@NotNull ContextHolder.Builder builder, @NotNull World world, @Nullable Player player) {
        CustomBlock block = this.owner.value();
        if (block == null) return List.of();
        LootTable<Object> lootTable = (LootTable<Object>) block.lootTable();
        if (lootTable == null) return List.of();
        return lootTable.getRandomItems(builder.withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, this).build(), world, player);
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> createSyncBlockEntityTicker(CEWorld world, BlockEntityType<? extends BlockEntity> type) {
        EntityBlockBehavior blockBehavior = this.behavior.getEntityBehavior();
        if (blockBehavior == null) return null;
        return (BlockEntityTicker<T>) blockBehavior.createSyncBlockEntityTicker(world, this, type);
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> createAsyncBlockEntityTicker(CEWorld world, BlockEntityType<? extends BlockEntity> type) {
        EntityBlockBehavior blockBehavior = this.behavior.getEntityBehavior();
        if (blockBehavior == null) return null;
        return (BlockEntityTicker<T>) blockBehavior.createAsyncBlockEntityTicker(world, this, type);
    }

    public Holder<CustomBlock> owner() {
        return this.owner;
    }

    public <T extends Comparable<T>> ImmutableBlockState cycle(Property<T> property, boolean backwards) {
        T currentValue = get(property);
        List<T> values = property.possibleValues();
        return with(property, getRelative(values, currentValue, backwards));
    }

    private static <T> T getRelative(List<T> values, T currentValue, boolean backwards) {
        return backwards ? MiscUtils.findPreviousInIterable(values, currentValue) : getNextValue(values, currentValue);
    }

    public <T extends Comparable<T>> ImmutableBlockState cycle(Property<T> property) {
        T currentValue = get(property);
        List<T> values = property.possibleValues();
        return with(property, getNextValue(values, currentValue));
    }

    private static <T> T getNextValue(List<T> values, T currentValue) {
        int index = values.indexOf(currentValue);
        if (index == -1) {
            throw new IllegalArgumentException("Current value not found in possible values");
        }
        return values.get((index + 1) % values.size());
    }

    @Override
    public String toString() {
        if (this.propertyMap.isEmpty()) {
            return this.owner.key().location().toString();
        }
        return this.owner.key().location() + "[" + getPropertiesAsString() + "]";
    }

    public String getPropertiesAsString() {
        return this.propertyMap.entrySet().stream()
                .map(entry -> {
                    Property<?> property = entry.getKey();
                    return property.name() + "=" + Property.formatValue(property, entry.getValue());
                })
                .collect(Collectors.joining(","));
    }

    public Collection<Property<?>> getProperties() {
        return Collections.unmodifiableSet(this.propertyMap.keySet());
    }

    public <T extends Comparable<T>> boolean contains(Property<T> property) {
        return this.propertyMap.containsKey(property);
    }

    public <T extends Comparable<T>> T get(Property<T> property) {
        T value = getNullable(property);
        if (value == null) {
            throw new IllegalArgumentException("Property " + property + " not found in " + this.owner.value().id());
        }
        return value;
    }

    public <T extends Comparable<T>> T get(Property<T> property, T fallback) {
        return Objects.requireNonNullElse(getNullable(property), fallback);
    }

    @Nullable
    public <T extends Comparable<T>> T getNullable(Property<T> property) {
        Comparable<?> value = this.propertyMap.get(property);
        return value != null ? property.valueClass().cast(value) : null;
    }

    public CompoundTag propertiesNbt() {
        CompoundTag properties = new CompoundTag();
        for (Map.Entry<Property<?>, Comparable<?>> entry : this.propertyMap.entrySet()) {
            Property<?> property = entry.getKey();
            properties.put(property.name(), pack(property, entry.getValue()));
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Tag pack(Property<T> property, Object value) {
        return property.pack((T) value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> ImmutableBlockState with(ImmutableBlockState state, Property<T> property, Object value) {
        return state.with(property, (T) value);
    }

    public ImmutableBlockState with(CompoundTag propertiesNBT) {
        CustomBlock owner = this.owner.value();
        ImmutableBlockState finalState = this;
        for (Map.Entry<String, Tag> entry : propertiesNBT.entrySet()) {
            Property<?> property = owner.getProperty(entry.getKey());
            if (property != null) {
                finalState = with(finalState, property, property.unpack(entry.getValue()));
            }
        }
        return finalState;
    }

    public <T extends Comparable<T>, V extends T> ImmutableBlockState with(Property<T> property, V value) {
        if (!this.propertyMap.containsKey(property)) {
            throw new IllegalArgumentException("Property " + property + " not found in " + this.owner.value().id());
        }
        return withInternal(property, value);
    }

    private <T extends Comparable<T>, V extends T> ImmutableBlockState withInternal(Property<T> property, V newValue) {
        if (newValue.equals(this.propertyMap.get(property))) {
            return this;
        }

        int index = property.indexOf(newValue);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid value " + newValue + " for property " + property);
        }

        return this.withMap.get(property)[index];
    }

    public void createWithMap(Map<Map<Property<?>, Comparable<?>>, ImmutableBlockState> states) {
        if (this.withMap != null) {
            throw new IllegalStateException("WithMap already initialized");
        }

        Reference2ObjectArrayMap<Property<?>, ImmutableBlockState[]> map = new Reference2ObjectArrayMap<>(this.propertyMap.size());

        for (Property<?> property : this.propertyMap.keySet()) {
            ImmutableBlockState[] statesArray = property.possibleValues().stream()
                    .map(value -> {
                        Map<Property<?>, Comparable<?>> testMap = new Reference2ObjectArrayMap<>(this.propertyMap);
                        testMap.put(property, value);
                        ImmutableBlockState state = states.get(testMap);
                        if (state == null) {
                            throw new IllegalStateException("Missing state for " + testMap);
                        }
                        return state;
                    })
                    .toArray(ImmutableBlockState[]::new);

            map.put(property, statesArray);
        }

        this.withMap = Map.copyOf(map);
    }

    public Map<Property<?>, Comparable<?>> propertyEntries() {
        return Collections.unmodifiableMap(this.propertyMap);
    }
}
