package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.behavior.SimpleStorageBlockBehavior;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.WorldlyContainerHolder;
import net.momirealms.craftengine.bukkit.world.inventory.BukkitWorldlyStorageContainer;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class SimpleStorageBlockEntityController extends BlockEntityController {
    private final SimpleStorageBlockBehavior behavior;
    private final Inventory inventory;
    private double maxInteractionDistance;
    private boolean openState = false;

    public SimpleStorageBlockEntityController(BlockEntity blockEntity, SimpleStorageBlockBehavior behavior) {
        super(blockEntity);
        this.behavior = behavior;
        WorldlyContainerHolder holder = new WorldlyContainerHolder(this::onPlayerClose, () -> new WorldPosition(blockEntity.world.world, blockEntity.pos.x + 0.5, blockEntity.pos.y + 0.5, blockEntity.pos.z + 0.5));
        this.inventory = CraftInventoryProxy.INSTANCE.newInstance(CraftEngine.instance().platform().createContainer(new SimpleStorageBlockContainer(holder, this.behavior.rows * 9, this.behavior.canPlaceItem, this.behavior.canTakeItem, blockEntity)));
        holder.setInventory(this.inventory);
    }

    @Override
    public void saveCustomData(CompoundTag tag) {
        // 保存前先把所有打开此容器的玩家界面关闭
        this.inventory.close();
        CompoundTag data = new CompoundTag();
        data.putInt("data_version", VersionHelper.WORLD_VERSION);
        data.put("items", ItemStackUtils.saveBukkitItemsAsListTag(this.inventory.getStorageContents()));
        tag.put(behavior.customDataKey, data);
    }

    @Override
    public void loadCustomData(CompoundTag tag) {
        // 应该优先读取新的数据，长期来看命中率更高
        CompoundTag dataTag = tag.getCompound(behavior.customDataKey);
        if (dataTag != null) {
            int dataVersion = dataTag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
            ListTag itemsTag = Optional.ofNullable(dataTag.getList("items")).orElseGet(ListTag::new);
            this.inventory.setStorageContents(ItemStackUtils.parseBukkitItems(itemsTag, this.behavior.rows * 9, dataVersion));
        } else {
            // 读取旧的
            ListTag oldItemsTag = tag.getList("items");
            if (oldItemsTag == null) return;
            ListTag itemsTag = Optional.ofNullable(tag.getList("items")).orElseGet(ListTag::new);
            int dataVersion = tag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
            this.inventory.setStorageContents(ItemStackUtils.parseBukkitItems(itemsTag, this.behavior.rows * 9, dataVersion));
        }
    }

    public Inventory inventory() {
        if (!super.blockEntity.isValid()) return null;
        return this.inventory;
    }

    public void onPlayerOpen(Player player) {
        if (!isValidContainer()) return;
        if (!player.isSpectatorMode()) {
            // 有非观察者的人，那么就不触发开启音效和事件
            if (!hasNoViewer(this.inventory.getViewers())) return;
            this.maxInteractionDistance = Math.max(player.getCachedInteractionRange(), this.maxInteractionDistance);
            this.setOpen(player);
            LevelAccessorProxy.INSTANCE.scheduleTick$0(super.blockEntity.world.world().minecraftWorld(), LocationUtils.toBlockPos(super.blockEntity.pos), BlockStateUtils.getBlockOwner(super.blockEntity.blockState.customBlockState().minecraftState()), 5);
        }
    }

    public void onPlayerClose(@Nullable Player player) {
        if (player == null || !isValidContainer()) return;
        if (!player.isSpectatorMode()) {
            // 有非观察者的人，那么就不触发关闭音效和事件
            for (HumanEntity viewer : this.inventory.getViewers()) {
                if (viewer.getGameMode() == GameMode.SPECTATOR || viewer == player.platformPlayer()) {
                    continue;
                }
                return;
            }
            this.maxInteractionDistance = 0;
            this.setClose(player);
        }
    }

    private void setOpen(@Nullable Player player) {
        this.updateOpenBlockState(true);
        org.bukkit.World bukkitWorld = (org.bukkit.World) super.blockEntity.world.world().platformWorld();
        if (player != null) {
            bukkitWorld.sendGameEvent((org.bukkit.entity.Player) player.platformPlayer(), GameEvent.CONTAINER_OPEN, new Vector(super.blockEntity.pos.x(), super.blockEntity.pos.y(), super.blockEntity.pos.z()));
        } else {
            bukkitWorld.sendGameEvent(null, GameEvent.CONTAINER_OPEN, new Vector(super.blockEntity.pos.x(), super.blockEntity.pos.y(), super.blockEntity.pos.z()));
        }
        this.openState = true;
        SoundData soundData = this.behavior.openSound;
        if (soundData != null) {
            super.blockEntity.world.world().playBlockSound(Vec3d.atCenterOf(super.blockEntity.pos), soundData);
        }
    }

    private void setClose(@Nullable Player player) {
        this.updateOpenBlockState(false);
        org.bukkit.World bukkitWorld = (org.bukkit.World) super.blockEntity.world.world().platformWorld();
        if (player != null) {
            bukkitWorld.sendGameEvent((org.bukkit.entity.Player) player.platformPlayer(), GameEvent.CONTAINER_CLOSE, new Vector(super.blockEntity.pos.x(), super.blockEntity.pos.y(), super.blockEntity.pos.z()));
        } else {
            bukkitWorld.sendGameEvent(null, GameEvent.CONTAINER_CLOSE, new Vector(super.blockEntity.pos.x(), super.blockEntity.pos.y(), super.blockEntity.pos.z()));
        }
        this.openState = false;
        SoundData soundData = this.behavior.closeSound;
        if (soundData != null) {
            super.blockEntity.world.world().playBlockSound(Vec3d.atCenterOf(super.blockEntity.pos), soundData);
        }
    }

    private boolean hasNoViewer(List<HumanEntity> viewers) {
        for (HumanEntity viewer : viewers) {
            if (viewer.getGameMode() != GameMode.SPECTATOR) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidContainer() {
        return super.blockEntity.isValid() && this.inventory != null && this.behavior != null;
    }

    public void updateOpenBlockState(boolean open) {
        ImmutableBlockState state = super.blockEntity.world.getBlockStateAtIfLoaded(super.blockEntity.pos);
        if (state == null || !super.blockEntity.isValidBlockState(state)) return;
        Property<Boolean> property = state.getProperty("open");
        if (property == null) return;
        super.blockEntity.world.world().setBlockState(super.blockEntity.pos.x(), super.blockEntity.pos.y(), super.blockEntity.pos.z(), state.with(property, open), UpdateFlags.UPDATE_ALL);
    }

    public void checkOpeners(Object level, Object pos, Object blockState) {
        if (!this.isValidContainer()) return;
        double maxInteractionDistance = 0d;
        List<HumanEntity> viewers = this.inventory.getViewers();
        int validViewers = 0;
        for (HumanEntity viewer : viewers) {
            if (viewer instanceof org.bukkit.entity.Player player) {
                BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                if (serverPlayer == null) continue;
                maxInteractionDistance = Math.max(serverPlayer.getCachedInteractionRange(), maxInteractionDistance);
                if (player.getGameMode() != GameMode.SPECTATOR) {
                    validViewers++;
                }
            }
        }
        boolean shouldOpen = validViewers != 0;
        if (shouldOpen && !this.openState) {
            this.setOpen(null);
        } else if (!shouldOpen && this.openState) {
            this.setClose(null);
        }

        this.maxInteractionDistance = maxInteractionDistance;
        if (!viewers.isEmpty()) {
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, pos, BlockStateUtils.getBlockOwner(blockState), 5);
        }
    }

    @Override
    public void onRemove() {
        this.inventory.close();
        Vec3d pos = Vec3d.atCenterOf(super.blockEntity.pos);
        for (ItemStack stack : this.inventory.getContents()) {
            if (stack != null) {
                super.blockEntity.world.world().dropItemNaturally(pos, BukkitItemManager.instance().wrap(stack));
            }
        }
        this.inventory.clear();
    }

    public static class SimpleStorageBlockContainer extends BukkitWorldlyStorageContainer {
        private final BlockEntity blockEntity;

        public SimpleStorageBlockContainer(InventoryHolder owner, int size, boolean canPlaceItem, boolean canTakeItem, BlockEntity blockEntity) {
            super(owner, size, canPlaceItem, canTakeItem);
            this.blockEntity = blockEntity;
        }

        @Override
        public void setChanged() {
            CEWorld ceWorld = blockEntity.world;
            if (ceWorld == null) return;
            ceWorld.blockEntityChanged(blockEntity.pos);
        }
    }
}
