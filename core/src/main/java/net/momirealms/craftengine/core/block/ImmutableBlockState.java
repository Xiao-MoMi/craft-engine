package net.momirealms.craftengine.core.block;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;

import javax.annotation.Nullable;
import java.util.List;

public class ImmutableBlockState extends BlockStateHolder {
    private CompoundTag tag;
    @Nullable
    private PackedBlockState customBlockState;
    @Nullable
    private PackedBlockState vanillaBlockState;

    private BlockBehavior behavior;
    private Integer hashCode;
    private BlockSettings settings;

    protected ImmutableBlockState(
            Holder<CustomBlock> owner,
            Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap
    ) {
        super(owner, propertyMap);
    }

    public BlockBehavior behavior() {
        return behavior;
    }

    public void setBehavior(BlockBehavior behavior) {
        this.behavior = behavior;
    }

    public BlockSettings settings() {
        return settings;
    }

    public void setSettings(BlockSettings settings) {
        this.settings = settings;
    }

    public boolean isEmpty() {
        return this == EmptyBlock.INSTANCE.getDefaultState();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImmutableBlockState state)) return false;
        return state.owner == this.owner && state.tag.equals(this.tag);
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = getNbtToSave().hashCode();
        }
        return hashCode;
    }

    public BlockSounds sounds() {
        return settings.sounds;
    }

    public int luminance() {
        return settings.luminance;
    }

    public PushReaction pushReaction() {
        return settings.pushReaction;
    }

    public PackedBlockState customBlockState() {
        return customBlockState;
    }

    public PackedBlockState vanillaBlockState() {
        return vanillaBlockState;
    }

    public void setCustomBlockState(@Nullable PackedBlockState customBlockState) {
        this.customBlockState = customBlockState;
    }

    public void setVanillaBlockState(@Nullable PackedBlockState vanillaBlockState) {
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
        if (tag == null) {
            tag = toNbtToSave(propertiesNbt());
        }
        return tag;
    }

    public CompoundTag toNbtToSave(CompoundTag properties) {
        CompoundTag tag = new CompoundTag();
        tag.put("properties", properties);
        tag.put("id", NBT.createString(owner.value().id().toString()));
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
    public List<Item<Object>> getDrops(ContextHolder.Builder builder, World world) {
        CustomBlock block = owner.value();
        if (block == null) return List.of();
        LootTable<Object> lootTable = (LootTable<Object>) block.lootTable();
        if (lootTable == null) return List.of();
        return lootTable.getRandomItems(builder.withParameter(LootParameters.BLOCK_STATE, this).build(), world);
    }
}
