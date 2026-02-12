package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.block.entity.BukkitBlockEntityTypes;
import net.momirealms.craftengine.bukkit.block.entity.SimpleStorageBlockEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.gui.BukkitInventory;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityType;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class SimpleStorageBlockBehavior extends BukkitBlockBehavior implements EntityBlockBehavior {
    public static final BlockBehaviorFactory<SimpleStorageBlockBehavior> FACTORY = new Factory();
    private final String containerTitle;
    private final int rows;
    private final SoundData openSound;
    private final SoundData closeSound;
    private final boolean hasAnalogOutputSignal;
    private final boolean canPlaceItem;
    private final boolean canTakeItem;
    @Nullable
    private final Property<Boolean> openProperty;

    public SimpleStorageBlockBehavior(CustomBlock customBlock,
                                      String containerTitle,
                                      int rows,
                                      SoundData openSound,
                                      SoundData closeSound,
                                      boolean hasAnalogOutputSignal,
                                      boolean canPlaceItem,
                                      boolean canTakeItem,
                                      @Nullable Property<Boolean> openProperty) {
        super(customBlock);
        this.containerTitle = containerTitle;
        this.rows = rows;
        this.openSound = openSound;
        this.closeSound = closeSound;
        this.hasAnalogOutputSignal = hasAnalogOutputSignal;
        this.canPlaceItem = canPlaceItem;
        this.canTakeItem = canTakeItem;
        this.openProperty = openProperty;
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
        if (!(blockEntity instanceof SimpleStorageBlockEntity entity) || entity.inventory() == null) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        entity.onPlayerOpen(player);
        bukkitPlayer.openInventory(entity.inventory());
        new BukkitInventory(entity.inventory()).open(player, AdventureHelper.miniMessage().deserialize(this.containerTitle, PlayerOptionalContext.of(player).tagResolvers()));
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    // 1.21.5+
    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object level = args[1];
        Object pos = args[2];
        Object blockState = args[0];
        FastNMS.INSTANCE.method$Level$updateNeighbourForOutputSignal(level, pos, BlockStateUtils.getBlockOwner(blockState));
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world = args[1];
        Object blockPos = args[2];
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        World bukkitWorld = FastNMS.INSTANCE.method$Level$getCraftWorld(world);
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(bukkitWorld.getUID());
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity instanceof SimpleStorageBlockEntity entity) {
            entity.checkOpeners(world, blockPos, args[0]);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(ImmutableBlockState state) {
        return EntityBlockBehavior.blockEntityTypeHelper(BukkitBlockEntityTypes.SIMPLE_STORAGE);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, ImmutableBlockState state) {
        return new SimpleStorageBlockEntity(pos, state);
    }

    @NotNull
    public String containerTitle() {
        return this.containerTitle;
    }

    @Nullable
    public SoundData closeSound() {
        return this.closeSound;
    }

    @Nullable
    public SoundData openSound() {
        return this.openSound;
    }

    public int rows() {
        return this.rows;
    }

    public boolean canPlaceItem() {
        return this.canPlaceItem;
    }

    public boolean canTakeItem() {
        return this.canTakeItem;
    }

    public @Nullable Property<Boolean> openProperty() {
        return openProperty;
    }

    @Override
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) {
        if (!this.hasAnalogOutputSignal) return 0;
        Object world = args[1];
        Object blockPos = args[2];
        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        World bukkitWorld = FastNMS.INSTANCE.method$Level$getCraftWorld(world);
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(bukkitWorld.getUID());
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(pos);
        if (blockEntity instanceof SimpleStorageBlockEntity entity) {
            Inventory inventory = entity.inventory();
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
        }
        return 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) {
        return this.hasAnalogOutputSignal;
    }

    @Override
    public Object getContainer(Object thisBlock, Object[] args) {
        CEWorld ceWorld = BukkitWorldManager.instance().getWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(args[1]));
        BlockPos blockPos = LocationUtils.fromBlockPos(args[2]);
        BlockEntity blockEntity = ceWorld.getBlockEntityAtIfLoaded(blockPos);
        if (blockEntity instanceof SimpleStorageBlockEntity entity) {
            return FastNMS.INSTANCE.method$CraftInventory$getInventory(entity.inventory());
        }
        return null;
    }

    private static class Factory implements BlockBehaviorFactory<SimpleStorageBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public SimpleStorageBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String title = arguments.getOrDefault("title", "").toString();
            int rows = MiscUtils.clamp(ResourceConfigUtils.getAsInt(arguments.getOrDefault("rows", 1), "rows"), 1, 6);
            Map<String, Object> sounds = (Map<String, Object>) arguments.get("sounds");
            boolean hasAnalogOutputSignal = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("has-signal", true), "has-signal");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (sounds != null) {
                openSound = Optional.ofNullable(sounds.get("open")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                closeSound = Optional.ofNullable(sounds.get("close")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_0_5, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            boolean canPlaceItem = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("allow-input", true), "allow-input");
            boolean canTakeItem = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("allow-output", true), "allow-output");
            Property<Boolean> property = (Property<Boolean>) block.getProperty("open");
            return new SimpleStorageBlockBehavior(block, title, rows, openSound, closeSound, hasAnalogOutputSignal, canPlaceItem, canTakeItem, property);
        }
    }
}
