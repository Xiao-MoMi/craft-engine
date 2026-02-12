package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.block.entity.BukkitBlockEntityTypes;
import net.momirealms.craftengine.bukkit.block.entity.ItemFrameBlockEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityType;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ItemFrameBlockBehavior extends BukkitBlockBehavior implements EntityBlockBehavior {
    public static final BlockBehaviorFactory<ItemFrameBlockBehavior> FACTORY = new Factory();
    public final Vector3f position;
    public final boolean glow;
    public final boolean invisible;
    public final boolean renderMapItem;
    public final SoundData putSound;
    public final SoundData takeSound;
    public final SoundData rotateSound;
    public final Property<Direction> directionProperty;

    public ItemFrameBlockBehavior(
            CustomBlock customBlock,
            Vector3f position,
            boolean glow,
            boolean invisible,
            boolean renderMapItem,
            SoundData putSound,
            SoundData takeSound,
            SoundData rotateSound,
            Property<Direction> directionProperty) {
        super(customBlock);
        this.position = position;
        this.glow = glow;
        this.invisible = invisible;
        this.renderMapItem = renderMapItem;
        this.putSound = putSound;
        this.takeSound = takeSound;
        this.rotateSound = rotateSound;
        this.directionProperty = directionProperty;
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(ImmutableBlockState state) {
        return EntityBlockBehavior.blockEntityTypeHelper(BukkitBlockEntityTypes.ITEM_FRAME);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, ImmutableBlockState state) {
        return new ItemFrameBlockEntity(pos, state);
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return getSignal(args[0], args[1], args[2], args[3]);
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return getSignal(args[0], args[1], args[2], args[3]);
    }

    private static int getSignal(Object blockState, Object blockAccess, Object pos, Object side) {
        if (!CoreReflections.clazz$Level.isInstance(blockAccess)) {
            return 0;
        }
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (state == null) {
            return 0;
        }
        ItemFrameBlockBehavior blockBehavior = state.behavior().getAs(ItemFrameBlockBehavior.class).orElse(null);
        if (blockBehavior == null) {
            return 0;
        }
        if (state.get(blockBehavior.directionProperty) != DirectionUtils.fromNMSDirection(side)) {
            return 0;
        }
        BukkitWorld world = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(blockAccess));
        BlockEntity blockEntity = world.storageWorld().getBlockEntityAtIfLoaded(LocationUtils.fromBlockPos(pos));
        if (!(blockEntity instanceof ItemFrameBlockEntity itemFrame && itemFrame.isValid())) {
            return 0;
        }
        if (ItemUtils.isEmpty(itemFrame.item())) {
            return 0;
        }
        return itemFrame.rotation() + 1;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return true;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = world.storageWorld().getBlockEntityAtIfLoaded(pos);
        if (!(blockEntity instanceof ItemFrameBlockEntity itemFrame && itemFrame.isValid())) {
            return InteractionResult.PASS;
        }
        Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_CONTAINER, location)) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        // 方块实体内部有物品的时候在shift时旋转
        if (player.isSecondaryUseActive() && !ItemUtils.isEmpty(itemFrame.item())) {
            itemFrame.rotation(itemFrame.rotation() + 1);
            playSound(world, pos, this.rotateSound);
            player.swingHand(context.getHand());
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        // 当主手为空的时候右键取下
        if (context.getHand() == InteractionHand.MAIN_HAND && ItemUtils.isEmpty(context.getItem())) {
            Item<ItemStack> item = itemFrame.item();
            if (ItemUtils.isEmpty(item)) { // 空的不管
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            itemFrame.updateItem(null); // 先取出来
            if (!player.canInstabuild()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, item); // 然后给玩家
            }
            playSound(world, pos, this.takeSound);
            player.swingHand(context.getHand());
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        // 当方块实体内部没有物品切换手上物品不为空则放入
        if (ItemUtils.isEmpty(itemFrame.item()) && !ItemUtils.isEmpty(context.getItem())) {
            @SuppressWarnings("unchecked")
            Item<ItemStack> item = (Item<ItemStack>) context.getItem();
            Item<ItemStack> copied = item.copyWithCount(1);
            if (!player.canInstabuild()) {
                item.shrink(1); // 先扣物品
            }
            itemFrame.updateItem(copied); // 然后放进去
            playSound(world, pos, this.putSound);
            player.swingHand(context.getHand());
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    private static void playSound(World world, BlockPos pos, SoundData soundData) {
        if (soundData == null) return;
        Vec3d location = new Vec3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        world.playBlockSound(location, soundData);
    }

    private static class Factory implements BlockBehaviorFactory<ItemFrameBlockBehavior> {

        @Override
        public ItemFrameBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            @SuppressWarnings("unchecked")
            Property<Direction> directionProperty = (Property<Direction>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.item_frame.missing_facing");
            Vector3f position = ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", 0), "position");
            boolean glow = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("glow", false), "glow");
            boolean invisible = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("invisible", false), "invisible");
            boolean renderMapItem = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("render-map-item", true), "render-map-item"); // 地图渲染有少量开销可选启用
            Map<String, Object> sounds = ResourceConfigUtils.getAsMapOrNull(arguments.get("sounds"), "sounds");
            SoundData putSound = null;
            SoundData takeSound = null;
            SoundData rotateSound = null;
            if (sounds != null) {
                putSound = Optional.ofNullable(sounds.get("put")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                takeSound = Optional.ofNullable(sounds.get("take")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                rotateSound = Optional.ofNullable(sounds.get("rotate")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new ItemFrameBlockBehavior(block, position, glow, invisible, renderMapItem, putSound, takeSound, rotateSound, directionProperty);
        }
    }
}
