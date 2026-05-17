package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.ItemFrameBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.renderer.dynamic.DynamicItemFrameBlockEntityElement;
import net.momirealms.craftengine.bukkit.entity.data.decoration.ItemFrameData;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.MapItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ItemFrameBlockEntityController extends BlockEntityController {
    public final ItemFrameBlockBehavior behavior;
    private final DynamicItemFrameBlockEntityElement element;
    private int rotation = 0;
    private @NotNull Object itemStack = ItemStackProxy.EMPTY;
    private @NotNull List<Object> cacheMetadata = List.of();
    private @Nullable Object mapId;
    private @Nullable Object mapItemSavedData;

    public ItemFrameBlockEntityController(BlockEntity blockEntity, ItemFrameBlockBehavior behavior) {
        super(blockEntity);
        this.behavior = behavior;
        this.element = new DynamicItemFrameBlockEntityElement(this, blockEntity.pos);
    }

    @Override
    public boolean hasElement() {
        return true;
    }

    @Override
    public void gatherElements(Consumer<BlockEntityElement> consumer) {
        consumer.accept(this.element);
    }

    @Override
    public void saveCustomData(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        data.putInt("rotation", this.rotation);
        data.putInt("data_version", VersionHelper.WORLD_VERSION);
        if (!ItemStackProxy.INSTANCE.isEmpty(this.itemStack)) {
            Tag itemTag = ItemStackUtils.saveMinecraftItemStackAsTag(this.itemStack);
            if (itemTag != null) data.put("item", itemTag);
        }
        tag.put(behavior.customDataKey, data);
    }

    @Override
    public void loadCustomData(CompoundTag tag) {
        // 应该优先读取新的数据，长期来看命中率更高
        CompoundTag dataTag = tag.getCompound(behavior.customDataKey);
        if (dataTag != null) {
            this.rotation = dataTag.getInt("rotation");
            int dataVersion = dataTag.getInt("data_version", VersionHelper.WORLD_VERSION);
            Tag itemTag = dataTag.get("item");
            if (itemTag == null) return;
            Object itemStack = ItemStackUtils.parseMinecraftItem(itemTag, dataVersion);
            if (itemStack == null) return;
            this.itemStack = itemStack;
        } else {
            // 读取旧的
            Tag oldItemTag = tag.get("item");
            if (oldItemTag == null) return;
            this.rotation = tag.getInt("rotation");
            int dataVersion = tag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
            Object itemStack = ItemStackUtils.parseMinecraftItem(oldItemTag, dataVersion);
            if (itemStack == null) return;
            this.itemStack = itemStack;
        }
        this.updateMetadata();
    }

    @Override
    public void onRemove() {
        if (ItemStackProxy.INSTANCE.isEmpty(this.itemStack)) return;
        super.blockEntity.world.world().dropItemNaturally(Vec3d.atCenterOf(super.blockEntity.pos), ItemStackUtils.wrap(this.itemStack));
    }

    public void updateItem(Item item) {
        if (item == null) {
            this.itemStack = ItemStackProxy.EMPTY;
        } else {
            this.itemStack = item.minecraftItem();
        }
        this.update();
    }

    public void rotation(int rotation) {
        this.rotation = rotation % 8;
        this.update();
    }

    public Item item() {
        return ItemStackUtils.wrap(this.itemStack);
    }

    public int rotation() {
        return this.rotation;
    }

    public List<Object> cacheMetadata() {
        return this.cacheMetadata;
    }

    @Nullable
    public Object mapId() {
        return this.mapId;
    }

    @Nullable
    public Object mapItemSavedData() {
        return this.mapItemSavedData;
    }

    public void setMapItemSavedData(@Nullable Object data) {
        this.mapItemSavedData = data;
    }

    private void update() {
        super.blockEntity.world.blockEntityChanged(super.blockEntity.pos);
        LevelProxy.INSTANCE.updateNeighbourForOutputSignal(
                super.blockEntity.world.world.minecraftWorld(),
                LocationUtils.toBlockPos(super.blockEntity.pos),
                BlockStateUtils.getBlockOwner(super.blockEntity.blockState.customBlockState().minecraftState())
        );
        CEChunk chunk = super.blockEntity.world.getChunkAtIfLoaded(super.blockEntity.pos.x >> 4, super.blockEntity.pos.z >> 4);
        if (chunk == null) return;
        this.updateMetadata();
        for (Player player : chunk.getTrackedBy()) {
            this.element.update(player);
        }
    }

    private void updateMetadata() {
        Object direction = DirectionUtils.toNMSDirection(super.blockEntity.blockState.get(this.behavior.directionProperty));
        List<Object> metadataValues = new ArrayList<>();
        ItemFrameData.Item.addEntityData(this.itemStack, metadataValues);
        ItemFrameData.Rotation.addEntityData(this.rotation, metadataValues);
        if (VersionHelper.isOrAbove1_21_6) {
            ItemFrameData.Direction.addEntityData(direction, metadataValues);
        }
        if (this.behavior.invisible) {
            ItemFrameData.SharedFlags.addEntityData((byte) 0x20, metadataValues);
        }
        this.cacheMetadata = metadataValues;
        if (this.behavior.renderMapItem) {
            if (VersionHelper.isOrAbove1_20_5) {
                this.mapId = ItemStackProxy.INSTANCE.get(this.itemStack, DataComponentTypes.MAP_ID);
            } else {
                this.mapId = MapItemProxy.INSTANCE.getMapId(this.itemStack);
            }
            if (this.mapId == null || super.blockEntity.world == null) {
                this.mapItemSavedData = null;
                return;
            }
            if (VersionHelper.isOrAbove1_20_5) {
                this.mapItemSavedData = MapItemProxy.INSTANCE.getSavedData$0(this.mapId, super.blockEntity.world.world.minecraftWorld());
            } else {
                this.mapItemSavedData = MapItemProxy.INSTANCE.getSavedData$1((Integer) this.mapId, super.blockEntity.world.world.minecraftWorld());
            }
        }
    }
}
