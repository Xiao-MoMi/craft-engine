package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.block.behavior.SimpleStorageBlockBehavior;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.nms.StorageContainer;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SimpleStorageBlockEntity extends BlockEntity {
    private final SimpleStorageBlockBehavior behavior;
    private final Inventory inventory;
    private double maxInteractionDistance;
    private boolean openState = false;

    public SimpleStorageBlockEntity(BlockPos pos, ImmutableBlockState blockState) {
        super(BukkitBlockEntityTypes.SIMPLE_STORAGE, pos, blockState);
        this.behavior = super.blockState.behavior().getAs(SimpleStorageBlockBehavior.class).orElseThrow();
        BlockEntityHolder holder = new BlockEntityHolder(this);
        this.inventory = FastNMS.INSTANCE.createSimpleStorageContainer(holder, this.behavior.rows() * 9, this.behavior.canPlaceItem(), this.behavior.canTakeItem());
        holder.setInventory(this.inventory);
        StorageContainer container = (StorageContainer) FastNMS.INSTANCE.method$CraftInventory$getInventory(this.inventory);
        container.onContentsChanged($ -> {
            CEWorld ceWorld = super.world;
            if (ceWorld == null) return;
            ceWorld.blockEntityChanged(pos);
        });
    }

    @Override
    protected void saveCustomData(CompoundTag tag) {
        // 保存前先把所有打开此容器的玩家界面关闭
        this.inventory.close();
        ListTag itemsTag = new ListTag();
        @Nullable ItemStack[] storageContents = this.inventory.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            if (storageContents[i] == null || !(ItemStackUtils.saveItemStackAsTag(storageContents[i]) instanceof CompoundTag itemTag)) {
                continue;
            }
            itemTag.putInt("slot", i);
            itemsTag.add(itemTag);
        }
        tag.put("items", itemsTag);
        tag.putInt("data_version", VersionHelper.WORLD_VERSION);
    }

    @Override
    public void loadCustomData(CompoundTag tag) {
        ListTag itemsTag = Optional.ofNullable(tag.getList("items")).orElseGet(ListTag::new);
        int dataVersion = tag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
        ItemStack[] storageContents = new ItemStack[this.behavior.rows() * 9];
        for (int i = 0; i < itemsTag.size(); i++) {
            CompoundTag itemTag = itemsTag.getCompound(i);
            int slot = itemTag.getInt("slot");
            if (slot < 0 || slot >= storageContents.length) {
                continue;
            }
            storageContents[slot] = ItemStackUtils.parseItemStack(itemTag, dataVersion);
        }
        this.inventory.setStorageContents(storageContents);
    }

    public Inventory inventory() {
        if (!isValid()) return null;
        return this.inventory;
    }

    public void onPlayerOpen(Player player) {
        if (!isValidContainer()) return;
        if (!player.isSpectatorMode()) {
            // 有非观察者的人，那么就不触发开启音效和事件
            if (!hasNoViewer(this.inventory.getViewers())) return;
            this.maxInteractionDistance = Math.max(player.getCachedInteractionRange(), this.maxInteractionDistance);
            this.setOpen(player);
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(super.world.world().serverWorld(), LocationUtils.toBlockPos(this.pos), BlockStateUtils.getBlockOwner(this.blockState.customBlockState().literalObject()), 5);
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
        org.bukkit.World bukkitWorld = (org.bukkit.World) super.world.world().platformWorld();
        if (player != null) {
            bukkitWorld.sendGameEvent((org.bukkit.entity.Player) player.platformPlayer(), GameEvent.CONTAINER_OPEN, new Vector(this.pos.x(), this.pos.y(), this.pos.z()));
        } else {
            bukkitWorld.sendGameEvent(null, GameEvent.CONTAINER_OPEN, new Vector(this.pos.x(), this.pos.y(), this.pos.z()));
        }
        this.openState = true;
        SoundData soundData = this.behavior.openSound();
        if (soundData != null) {
            super.world.world().playBlockSound(Vec3d.atCenterOf(this.pos), soundData);
        }
    }

    private void setClose(@Nullable Player player) {
        this.updateOpenBlockState(false);
        org.bukkit.World bukkitWorld = (org.bukkit.World) super.world.world().platformWorld();
        if (player != null) {
            bukkitWorld.sendGameEvent((org.bukkit.entity.Player) player.platformPlayer(), GameEvent.CONTAINER_CLOSE, new Vector(this.pos.x(), this.pos.y(), this.pos.z()));
        } else {
            bukkitWorld.sendGameEvent(null, GameEvent.CONTAINER_CLOSE, new Vector(this.pos.x(), this.pos.y(), this.pos.z()));
        }
        this.openState = false;
        SoundData soundData = this.behavior.closeSound();
        if (soundData != null) {
            super.world.world().playBlockSound(Vec3d.atCenterOf(this.pos), soundData);
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
        return this.isValid() && this.inventory != null && this.behavior != null;
    }

    public void updateOpenBlockState(boolean open) {
        ImmutableBlockState state = super.world.getBlockStateAtIfLoaded(this.pos);
        if (state == null) return;
        SimpleStorageBlockBehavior behavior = state.behavior().getAs(SimpleStorageBlockBehavior.class).orElse(null);
        if (behavior == null) return;
        Property<Boolean> property = behavior.openProperty();
        if (property == null) return;
        super.world.world().setBlockState(this.pos.x(), this.pos.y(), this.pos.z(), state.with(property, open), UpdateOption.UPDATE_ALL.flags());
    }

    public void checkOpeners(Object level, Object pos, Object blockState) {
        if (!this.isValidContainer()) return;
        double maxInteractionDistance = 0d;
        List<HumanEntity> viewers = this.inventory.getViewers();
        int validViewers = 0;
        for (HumanEntity viewer : viewers) {
            if (viewer instanceof org.bukkit.entity.Player player) {
                BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
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
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(level, pos, BlockStateUtils.getBlockOwner(blockState), 5);
        }
    }

    @Override
    public void preRemove() {
        this.inventory.close();
        Vec3d pos = Vec3d.atCenterOf(this.pos);
        for (ItemStack stack : this.inventory.getContents()) {
            if (stack != null) {
                super.world.world().dropItemNaturally(pos, BukkitItemManager.instance().wrap(stack));
            }
        }
        this.inventory.clear();
    }
}
