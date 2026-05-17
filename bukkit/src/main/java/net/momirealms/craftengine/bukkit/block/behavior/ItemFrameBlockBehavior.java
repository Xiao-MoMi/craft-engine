package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.entity.ItemFrameBlockEntityController;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import org.bukkit.Location;
import org.joml.Vector3f;

public final class ItemFrameBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<ItemFrameBlockBehavior> FACTORY = new Factory();
    public final Vector3f position;
    public final boolean glow;
    public final boolean invisible;
    public final boolean renderMapItem;
    public final SoundData putSound;
    public final SoundData takeSound;
    public final SoundData rotateSound;
    public final Property<Direction> directionProperty;
    private int controllerId;
    public final String customDataKey;

    private ItemFrameBlockBehavior(BlockDefinition blockDefinition,
                                   Vector3f position,
                                   boolean glow,
                                   boolean invisible,
                                   boolean renderMapItem,
                                   SoundData putSound,
                                   SoundData takeSound,
                                   SoundData rotateSound,
                                   Property<Direction> directionProperty,
                                   String customDataKey
    ) {
        super(blockDefinition);
        this.position = position;
        this.glow = glow;
        this.invisible = invisible;
        this.renderMapItem = renderMapItem;
        this.putSound = putSound;
        this.takeSound = takeSound;
        this.rotateSound = rotateSound;
        this.directionProperty = directionProperty;
        this.customDataKey = customDataKey;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new ItemFrameBlockEntityController(blockEntity, this);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args) {
        return getSignal(args[0], args[1], args[2], args[3]);
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args) {
        return getSignal(args[0], args[1], args[2], args[3]);
    }

    private int getSignal(Object blockState, Object blockAccess, Object pos, Object side) {
        if (!LevelProxy.CLASS.isInstance(blockAccess)) {
            return 0;
        }
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (state == null) {
            return 0;
        }
        if (state.get(this.directionProperty) != DirectionUtils.fromNMSDirection(side)) {
            return 0;
        }
        BukkitWorld world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(blockAccess));
        BlockEntity blockEntity = world.storageWorld().getBlockEntityAtIfLoaded(LocationUtils.fromBlockPos(pos));
        if (blockEntity == null) {
            return 0;
        }
        return blockEntity.controller.let(ItemFrameBlockEntityController.class, this.controllerId, c -> {
            if (ItemUtils.isEmpty(c.item())) {
                return 0;
            }
            return c.rotation() + 1;
        });
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = world.storageWorld().getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }
        Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_CONTAINER, location)) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        return blockEntity.controller.let(ItemFrameBlockEntityController.class, this.controllerId, itemFrame -> {
            // 方块实体内部有物品的时候在shift时旋转
            if (player.isSecondaryUseActive() && !ItemUtils.isEmpty(itemFrame.item())) {
                itemFrame.rotation(itemFrame.rotation() + 1);
                playSound(world, pos, this.rotateSound);
                player.swingHand(context.getHand());
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            // 当主手为空的时候右键取下
            if (context.getHand() == InteractionHand.MAIN_HAND && ItemUtils.isEmpty(context.getItem())) {
                Item item = itemFrame.item();
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
                Item item = context.getItem();
                Item copied = item.copyWithCount(1);
                if (!player.canInstabuild()) {
                    item.shrink(1); // 先扣物品
                }
                itemFrame.updateItem(copied); // 然后放进去
                playSound(world, pos, this.putSound);
                player.swingHand(context.getHand());
                return InteractionResult.SUCCESS_AND_CANCEL;
            }

            return InteractionResult.SUCCESS_AND_CANCEL;
        });
    }

    private static void playSound(World world, BlockPos pos, SoundData soundData) {
        if (soundData == null) return;
        Vec3d location = new Vec3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        world.playBlockSound(location, soundData);
    }

    private static class Factory implements BlockBehaviorFactory<ItemFrameBlockBehavior> {
        private static final String[] RENDER_MAP_ITEM = new String[]{"render_map_item", "render-map-item"};
        private static final String[] DATA_KEY = new String[] {"data_key", "data-key"};

        @Override
        public ItemFrameBlockBehavior create(BlockDefinition block, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            SoundData putSound = null;
            SoundData takeSound = null;
            SoundData rotateSound = null;
            if (soundSection != null) {
                putSound = soundSection.getValue("put", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                takeSound = soundSection.getValue("take", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                rotateSound = soundSection.getValue("rotate", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new ItemFrameBlockBehavior(
                    block,
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getBoolean("glow"),
                    section.getBoolean("invisible"),
                    section.getBoolean(RENDER_MAP_ITEM, true),
                    putSound,
                    takeSound,
                    rotateSound,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    section.getValue(DATA_KEY, ConfigValue::getAsNonEmptyString, "craftengine:item_frame")
            );
        }
    }
}
