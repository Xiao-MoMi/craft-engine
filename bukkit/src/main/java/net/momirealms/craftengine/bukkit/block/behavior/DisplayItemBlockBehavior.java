package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.block.entity.DisplayItemBlockEntityController;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
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
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class DisplayItemBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<DisplayItemBlockBehavior> FACTORY = new Factory();
    public final SoundData putSound;
    public final SoundData takeSound;
    public final boolean hasAnalogOutputSignal;
    public final Vector3f relativePosition;
    @Nullable
    public final Property<Direction> directionProperty;
    private int controllerId;
    public final String customDataKey;
    public final boolean tintSource;

    public DisplayItemBlockBehavior(BlockDefinition blockDefinition,
                                    SoundData putSound,
                                    SoundData takeSound,
                                    boolean hasAnalogOutputSignal,
                                    Vector3f relativePosition,
                                    boolean tintSource,
                                    @Nullable Property<Direction> directionProperty,
                                    String customDataKey
    ) {
        super(blockDefinition);
        this.putSound = putSound;
        this.takeSound = takeSound;
        this.hasAnalogOutputSignal = hasAnalogOutputSignal;
        this.relativePosition = relativePosition;
        this.directionProperty = directionProperty;
        this.customDataKey = customDataKey;
        this.tintSource = tintSource;
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return this.tintSource ? new DisplayItemBlockEntityController.Tintable(blockEntity, this) : new DisplayItemBlockEntityController(blockEntity, this);
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
        Location location = new Location((org.bukkit.World) context.getWorld().platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_CONTAINER, location)) {
            return InteractionResult.FAIL;
        }
        InteractionHand hand = context.getHand();
        Item itemInHand = player.getItemInHand(hand);
        return blockEntity.controller.let(DisplayItemBlockEntityController.class, this.controllerId, c -> {
            // 放入物品
            if (!ItemUtils.isEmpty(itemInHand) && ItemUtils.isEmpty(c.displayItem())) {
                Item inputItem = itemInHand.copyWithCount(1);
                if (!player.canInstabuild()) {
                    itemInHand.shrink(1);
                }
                c.putDisplayItem(inputItem);
                player.swingHand(hand);
                if (this.putSound != null) world.playBlockSound(Vec3d.atCenterOf(pos), this.putSound);
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            // 取出物品
            else if (ItemUtils.isEmpty(itemInHand) && !ItemUtils.isEmpty(c.displayItem())) {
                Item takedItem = c.takeDisplayItem();
                player.setItemInHand(hand, takedItem);
                player.swingHand(hand);
                if (this.takeSound != null) world.playBlockSound(Vec3d.atCenterOf(pos), this.takeSound);
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            return InteractionResult.TRY_EMPTY_HAND;
        });
    }

    // 比较器红石信号.
    @Override
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) {
        if (!this.hasAnalogOutputSignal) return 0;
        Object world = args[1];
        Object blockPos = args[2];
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        org.bukkit.World bukkitWorld = LevelProxy.INSTANCE.getWorld(world);
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(bukkitWorld.getUID());
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) {
            return 0;
        }
        return blockEntity.controller.let(DisplayItemBlockEntityController.class, this.controllerId, c -> {
            if (!ItemUtils.isEmpty(c.displayItem())) {
                return 15;
            }
            return 0;
        });
    }

    @Override
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) {
        return this.hasAnalogOutputSignal;
    }

    private static class Factory implements BlockBehaviorFactory<DisplayItemBlockBehavior> {
        private static final String[] HAS_SIGNAL = new String[]{"has_signal", "has-signal"};
        private static final String[] DATA_KEY = new String[] {"data_key", "data-key"};
        private static final String[] TINT_SOURCE = new String[] {"tint_source", "tint-source"};

        @Override
        public DisplayItemBlockBehavior create(BlockDefinition block, ConfigSection section) {
            // 读取展示相对位置
            Vector3f position = section.getVector3f("position", ConfigConstants.CENTER_VECTOR3);
            // 读取放入取出音效
            ConfigSection soundSection = section.getSection("sounds");
            SoundData putSound = null;
            SoundData takeSound = null;
            if (soundSection != null) {
                putSound = soundSection.getValue("put", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                takeSound = soundSection.getValue("take", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
            }
            // 获取方向
            Property<Direction> facing = BlockBehaviorFactory.getOptionalProperty(block, "facing", Direction.class);
            return new DisplayItemBlockBehavior(
                    block,
                    putSound,
                    takeSound,
                    section.getBoolean(HAS_SIGNAL, true),
                    position,
                    section.getBoolean(TINT_SOURCE, false),
                    facing,
                    section.getValue(DATA_KEY, ConfigValue::getAsNonEmptyString, "craftengine:display_item")
            );
        }
    }
}
