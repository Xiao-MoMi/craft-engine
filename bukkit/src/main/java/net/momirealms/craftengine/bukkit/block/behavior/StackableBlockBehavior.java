package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.IntegerProperty;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StackableBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final IntegerProperty amountProperty;
    private final List<Key> items;
    private final SoundData stackSound;

    public StackableBlockBehavior(CustomBlock block, IntegerProperty amountProperty, List<Key> items, SoundData stackSound) {
        super(block);
        this.amountProperty = amountProperty;
        this.items = items;
        this.stackSound = stackSound;
    }

    @Override
    @SuppressWarnings("unchecked")
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Item<ItemStack> item = (Item<ItemStack>) context.getItem();
        if (ItemUtils.isEmpty(item)) {
            return InteractionResult.PASS;
        }
        if (!this.items.contains(item.id())) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        World world = context.getLevel();
        if (state.get(this.amountProperty) >= this.amountProperty.max) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        updateStackableBlock(state, pos, world, item, player, context.getHand());
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    private void updateStackableBlock(ImmutableBlockState state, BlockPos pos, World world, Item<ItemStack> item, Player player, InteractionHand hand) {
        ImmutableBlockState nextStage = state.cycle(this.amountProperty);
        Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x(), pos.y(), pos.z());
        FastNMS.INSTANCE.method$LevelWriter$setBlock(world.serverWorld(), LocationUtils.toBlockPos(pos), nextStage.customBlockState().handle(), UpdateOption.UPDATE_NONE.flags());
        if (this.stackSound != null) {
            world.playBlockSound(new Vec3d(location.getX(), location.getY(), location.getZ()), this.stackSound);
        }
        if (!player.isCreativeMode()) {
            item.count(item.count() - 1);
        }
        player.swingHand(hand);
    }

    public static class Factory implements BlockBehaviorFactory {

        @Override
        @SuppressWarnings("unchecked")
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String propertyName = String.valueOf(arguments.getOrDefault("property", "amount"));
            IntegerProperty amount = (IntegerProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty(propertyName), () -> {
                throw new LocalizedResourceConfigException("warning.config.block.behavior.stackable.missing_property", propertyName);
            });
            Map<String, Object> sounds = (Map<String, Object>) arguments.get("sounds");
            SoundData stackSound = null;
            if (sounds != null) {
                stackSound = Optional.ofNullable(sounds.get("stack")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1)).orElse(null);
            }
            Object itemsObj = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("items"), "warning.config.block.behavior.stackable.missing_items");
            List<Key> items = MiscUtils.getAsStringList(itemsObj).stream().map(Key::of).toList();
            return new StackableBlockBehavior(block, amount, items, stackSound);
        }
    }
}
