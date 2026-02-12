package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.CanBeReplacedBlockBehavior;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;

import java.util.List;
import java.util.Map;

public class StackableBlockBehavior extends BukkitBlockBehavior implements CanBeReplacedBlockBehavior {
    public static final BlockBehaviorFactory<StackableBlockBehavior> FACTORY = new Factory();
    private final IntegerProperty amountProperty;
    private final List<Key> items;
    private final String propertyName;

    public StackableBlockBehavior(CustomBlock block, IntegerProperty amountProperty, List<Key> items, String propertyName) {
        super(block);
        this.amountProperty = amountProperty;
        this.items = items;
        this.propertyName = propertyName;
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        if (super.canBeReplaced(context, state)) {
            return true;
        }
        if (context.isSecondaryUseActive()) {
            return false;
        }
        Item<?> item = context.getItem();
        if (ItemUtils.isEmpty(item)) {
            return false;
        }
        if (!this.items.contains(item.id())) {
            return false;
        }
        Property<?> property = state.owner().value().getProperty(this.propertyName);
        if (property == null || property.valueClass() != Integer.class) {
            return false;
        }
        return (Integer) state.get(property) < this.amountProperty.max;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object world = context.getLevel().serverWorld();
        Object pos = LocationUtils.toBlockPos(context.getClickedPos());
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(BlockGetterProxy.INSTANCE.getBlockState(world, pos)).orElse(null);
        if (blockState == null) {
            return state;
        }
        Property<?> property = blockState.owner().value().getProperty(this.propertyName);
        if (property == null || property.valueClass() != Integer.class) {
            return state;
        }
        return blockState.cycle(property);
    }

    private static class Factory implements BlockBehaviorFactory<StackableBlockBehavior> {

        @Override
        public StackableBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String propertyName = String.valueOf(arguments.getOrDefault("property", "amount"));
            IntegerProperty amount = (IntegerProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty(propertyName), () -> {
                throw new LocalizedResourceConfigException("warning.config.block.behavior.stackable.missing_property", propertyName);
            });
            Object itemsObj = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("items"), "warning.config.block.behavior.stackable.missing_items");
            List<Key> items = MiscUtils.getAsStringList(itemsObj).stream().map(Key::of).toList();
            return new StackableBlockBehavior(block, amount, items, propertyName);
        }
    }
}
