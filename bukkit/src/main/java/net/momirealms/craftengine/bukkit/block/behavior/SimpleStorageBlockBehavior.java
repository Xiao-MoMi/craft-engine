package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.block.entity.SimpleStorageBlockEntityController;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.gui.BukkitInventory;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.behavior.WorldlyContainerHolder;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SimpleStorageBlockBehavior extends BukkitBlockBehavior implements EntityBlock, WorldlyContainerHolder {
    public static final BlockBehaviorFactory<SimpleStorageBlockBehavior> FACTORY = new Factory();
    public final String containerTitle;
    public final int rows;
    public final SoundData openSound;
    public final SoundData closeSound;
    public final boolean hasAnalogOutputSignal;
    public final boolean canPlaceItem;
    public final boolean canTakeItem;
    @Nullable
    public final Property<Boolean> openProperty;
    private int controllerId;
    public final String customDataKey;

    private SimpleStorageBlockBehavior(BlockDefinition blockDefinition,
                                       String containerTitle,
                                       int rows,
                                       SoundData openSound,
                                       SoundData closeSound,
                                       boolean hasAnalogOutputSignal,
                                       boolean canPlaceItem,
                                       boolean canTakeItem,
                                       @Nullable Property<Boolean> openProperty,
                                       String customDataKey
    ) {
        super(blockDefinition);
        this.containerTitle = containerTitle;
        this.rows = rows;
        this.openSound = openSound;
        this.closeSound = closeSound;
        this.hasAnalogOutputSignal = hasAnalogOutputSignal;
        this.canPlaceItem = canPlaceItem;
        this.canTakeItem = canTakeItem;
        this.openProperty = openProperty;
        this.customDataKey = customDataKey;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new SimpleStorageBlockEntityController(blockEntity, this);
    }

    @Override
    public void initControllerId(int id) {
        this.controllerId = id;
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        CEWorld world = context.getLevel().storageWorld();
        net.momirealms.craftengine.core.entity.player.Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        BlockPos blockPos = context.getClickedPos();
        World bukkitWorld = (World) context.getLevel().platformWorld();
        Location location = new Location(bukkitWorld, blockPos.x(), blockPos.y(), blockPos.z());
        Player bukkitPlayer = (Player) player.platformPlayer();
        if (!BukkitCraftEngine.instance().antiGriefProvider().test(bukkitPlayer, Flag.OPEN_CONTAINER, location)) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(blockPos);
        if (blockEntity == null) return InteractionResult.FAIL;
        blockEntity.controller.let(SimpleStorageBlockEntityController.class, this.controllerId, c -> {
            c.onPlayerOpen(player);
            new BukkitInventory(c.inventory()).open(player, AdventureHelper.miniMessage().deserialize(this.containerTitle, PlayerOptionalContext.of(player).tagResolvers()));
        });
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args) {
        Object level = args[1];
        Object pos = args[2];
        Object blockState = args[0];
        LevelProxy.INSTANCE.updateNeighbourForOutputSignal(level, pos, BlockStateUtils.getBlockOwner(blockState));
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        Object world = args[1];
        Object blockPos = args[2];
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        World bukkitWorld = LevelProxy.INSTANCE.getWorld(world);
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(bukkitWorld.getUID());
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) return;
        blockEntity.controller.let(SimpleStorageBlockEntityController.class, this.controllerId, c -> {
            c.checkOpeners(world, blockPos, args[0]);
        });
    }

    @Override
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) {
        if (!this.hasAnalogOutputSignal) return 0;
        Object world = args[1];
        Object blockPos = args[2];
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        World bukkitWorld = LevelProxy.INSTANCE.getWorld(world);
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(bukkitWorld.getUID());
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity == null) return 0;
        return blockEntity.controller.let(SimpleStorageBlockEntityController.class, this.controllerId, c -> {
            Inventory inventory = c.inventory();
            if (inventory != null) {
                float signal = 0.0F;
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null) {
                        signal += (float) item.getAmount() / (float) (Math.min(inventory.getMaxStackSize(), item.getMaxStackSize()));
                    }
                }
                signal /= (float) inventory.getSize();
                return MiscUtils.lerpDiscrete(signal, 0, 15);
            }
            return 0;
        });
    }

    @Override
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) {
        return this.hasAnalogOutputSignal;
    }

    @Override
    public Object getContainer(Object thisBlock, Object[] args) {
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(LevelProxy.INSTANCE.getWorld(args[1]));
        BlockPos blockPos = LocationUtils.fromBlockPos(args[2]);
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(blockPos);
        if (blockEntity == null) return null;
        return blockEntity.controller.let(SimpleStorageBlockEntityController.class, this.controllerId, c -> {
            return CraftInventoryProxy.INSTANCE.getInventory(c.inventory());
        });
    }

    private static class Factory implements BlockBehaviorFactory<SimpleStorageBlockBehavior> {
        private static final String[] HAS_SIGNAL = new String[]{"has_signal", "has-signal"};
        private static final String[] ALLOW_INPUT = new String[]{"allow_input", "allow-input"};
        private static final String[] ALLOW_OUTPUT = new String[]{"allow_output", "allow-output"};
        private static final String[] DATA_KEY = new String[] {"data_key", "data-key"};


        @Override
        public SimpleStorageBlockBehavior create(BlockDefinition block, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (soundSection != null) {
                openSound = soundSection.getValue("open", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.RANGED_0_9_1));
                closeSound = soundSection.getValue("close", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new SimpleStorageBlockBehavior(
                    block,
                    section.getString("title", "<lang:container.chest>"),
                    section.getInt("rows", 1),
                    openSound,
                    closeSound,
                    section.getBoolean(HAS_SIGNAL, true),
                    section.getBoolean(ALLOW_INPUT, true),
                    section.getBoolean(ALLOW_OUTPUT, true),
                    BlockBehaviorFactory.getOptionalProperty(block, "open", Boolean.class),
                    section.getValue(DATA_KEY, ConfigValue::getAsNonEmptyString, "craftengine:simple_storage")
            );
        }
    }
}
