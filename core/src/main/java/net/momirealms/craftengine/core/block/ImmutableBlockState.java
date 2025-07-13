package net.momirealms.craftengine.core.block;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ImmutableBlockState extends BlockStateHolder {
    private CompoundTag tag;
    private BlockStateWrapper.CustomBlockState customBlockState;
    private BlockStateWrapper.VanillaBlockState vanillaBlockState;
    private BlockBehavior behavior;
    private Integer hashCode;
    private BlockSettings settings;

    ImmutableBlockState(
            Holder<CustomBlock> owner,
            Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap
    ) {
        super(owner, propertyMap);
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

    public boolean isEmpty() {
        return this == EmptyBlock.STATE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableBlockState state)) return false;
        return state.owner == this.owner && state.tag.equals(this.tag);
    }

    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = getNbtToSave().hashCode();
        }
        return this.hashCode;
    }

    public BlockStateWrapper customBlockState() {
        return this.customBlockState;
    }

    public BlockStateWrapper vanillaBlockState() {
        return this.vanillaBlockState;
    }

    public void setCustomBlockState(@NotNull BlockStateWrapper.CustomBlockState customBlockState) {
        this.customBlockState = customBlockState;
    }

    public void setVanillaBlockState(@NotNull BlockStateWrapper.VanillaBlockState vanillaBlockState) {
        this.vanillaBlockState = vanillaBlockState;
    }

    public CompoundTag propertiesNbt() {
        CompoundTag properties = new CompoundTag();
        for (Property<?> property : getProperties()) {
            Comparable<?> value = get(property);
            if (value != null) {
                properties.put(property.name(), pack(property, value));
                continue;
            }
            properties.put(property.name(), pack(property, property.defaultValue()));
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Tag pack(Property<T> property, Object value) {
        return property.pack((T) value);
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
        tag.put("id", NBT.createString(this.owner.value().id().asString()));
        return tag;
    }

    public void setNbtToSave(CompoundTag tag) {
        this.tag = tag;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> ImmutableBlockState with(ImmutableBlockState state, Property<T> property, Object value) {
        return state.with(property, (T) value);
    }

    @SuppressWarnings("unchecked")
    public List<Item<Object>> getDrops(@NotNull ContextHolder.Builder builder, @NotNull World world, @Nullable Player player) {
        CustomBlock block = this.owner.value();
        if (block == null) return List.of();
        LootTable<Object> lootTable = (LootTable<Object>) block.lootTable();
        if (lootTable == null) return List.of();
        return lootTable.getRandomItems(builder.withParameter(DirectContextParameters.CUSTOM_BLOCK_STATE, this).build(), world, player);
    }
}
