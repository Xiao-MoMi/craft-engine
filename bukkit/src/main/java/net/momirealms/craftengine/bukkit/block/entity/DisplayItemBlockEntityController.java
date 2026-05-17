package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.DisplayItemBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.renderer.dynamic.DynamicDisplayItemBlockEntityElement;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.world.TintSource;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.IntTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class DisplayItemBlockEntityController extends BlockEntityController {
    private final DisplayItemBlockBehavior behavior;
    private final DynamicDisplayItemBlockEntityElement element;
    @NotNull
    private Item displayItem;
    private WorldPosition displayItemPosition;
    private final Vector3f blockCenter;

    public DisplayItemBlockEntityController(BlockEntity blockEntity, DisplayItemBlockBehavior behavior) {
        super(blockEntity);
        this.behavior = behavior;
        this.blockCenter = new Vector3f((float) (blockEntity.pos.x + 0.5), (float) (blockEntity.pos.y + 0.5), (float) (blockEntity.pos.z + 0.5));
        this.displayItem = Item.empty();
        this.displayItemPosition = this.calculateDisplayItemPosition(blockEntity.blockState);
        this.element = new DynamicDisplayItemBlockEntityElement(this, this.displayItemPosition);
    }

    @Override
    public boolean hasElement() {
        return true;
    }

    @Override
    public void gatherElements(Consumer<BlockEntityElement> consumer) {
        consumer.accept(this.element);
    }

    @NotNull
    public Item displayItem() {
        return this.displayItem;
    }

    /**
     * @param inputItem 不为空的物品
     */
    public void putDisplayItem(Item inputItem) {
        this.displayItem = inputItem;
        this.element.refreshChangeDisplayItemPacket(this.displayItem.minecraftItem());
        CEChunk chunk = super.blockEntity.world.getChunkAtIfLoaded(super.blockEntity.pos.x >> 4, super.blockEntity.pos.z >> 4);
        if (chunk != null) {
            for (Player trackedPlayer : chunk.getTrackedBy()) {
                this.element.showDisplayItem(trackedPlayer);
            }
        }
        super.blockEntity.world.blockEntityChanged(super.blockEntity.pos);
    }

    public Item takeDisplayItem() {
        Item temp = this.displayItem;
        this.displayItem = Item.empty();
        CEChunk chunk = super.blockEntity.world.getChunkAtIfLoaded(super.blockEntity.pos.x >> 4, super.blockEntity.pos.z >> 4);
        if (chunk != null) {
            for (Player trackedPlayer : chunk.getTrackedBy()) {
                this.element.hide(trackedPlayer);
            }
        }
        super.blockEntity.world.blockEntityChanged(super.blockEntity.pos);
        return temp;
    }

    @Override
    public void preBlockStateChange(ImmutableBlockState newState) {
        this.displayItemPosition = this.calculateDisplayItemPosition(newState);
        this.element.refreshSpawnVehicleAndPassengerPacket(this.displayItemPosition);
        this.element.refreshChangeDisplayItemPacket(this.displayItem.minecraftItem());
        CEChunk chunk = super.blockEntity.world.getChunkAtIfLoaded(super.blockEntity.pos.x >> 4, super.blockEntity.pos.z >> 4);
        if (chunk != null) {
            for (Player trackedPlayer : chunk.getTrackedBy()) {
                this.element.updateElementPos(trackedPlayer);
                this.element.refreshDisplayItem(trackedPlayer);
            }
        }
    }

    // 读取方块内存储的物品
    @Override
    public void loadCustomData(CompoundTag tag) {
        CompoundTag dataTag = tag.getCompound(behavior.customDataKey);
        // 空数据
        if (dataTag == null) {
            this.displayItem = Item.empty();
            return;
        }
        // 读取数据
        int dataVersion = dataTag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
        Tag itemTag = dataTag.get("display_item");
        // 非法数据
        if (itemTag == null) {
            this.displayItem = Item.empty();
            return;
        }
        // 记录并刷新
        this.displayItem = ItemStackUtils.wrap(ItemStackUtils.parseMinecraftItem(itemTag, dataVersion));
        this.element.refreshChangeDisplayItemPacket(this.displayItem.minecraftItem());
    }

    @Override
    public void saveCustomData(CompoundTag tag) {
        if (ItemUtils.isEmpty(displayItem)) return;
        CompoundTag data = new CompoundTag();
        data.put("data_version", new IntTag(Config.itemDataFixerUpperFallbackVersion()));
        data.put("display_item", ItemStackUtils.saveMinecraftItemStackAsTag(this.displayItem.minecraftItem()));
        tag.put(behavior.customDataKey, data);
    }

    @Override
    public void onRemove() {
        if (!ItemUtils.isEmpty(displayItem)) {
            super.blockEntity.world.world().dropItemNaturally(this.displayItemPosition, this.displayItem);
        }
        this.displayItem = Item.empty();
    }

    public WorldPosition calculateDisplayItemPosition(ImmutableBlockState blockState) {
        float angleDeg;
        Direction direction = blockState.get(behavior.directionProperty, Direction.SOUTH);
        switch (direction.opposite()) {
            case EAST -> angleDeg = 90f;
            case SOUTH -> angleDeg = 180f;
            case WEST -> angleDeg = 270f;
            default -> angleDeg = 0f;
        }
        double angleRad = Math.toRadians(angleDeg);

        float x = -this.behavior.relativePosition.x;
        float z = this.behavior.relativePosition.z;
        double rotatedX = x * Math.cos(angleRad) - z * Math.sin(angleRad);
        double rotatedZ = x * Math.sin(angleRad) + z * Math.cos(angleRad);

        return new WorldPosition(null,
                this.blockCenter.x + rotatedX,
                this.blockCenter.y + this.behavior.relativePosition.y,
                this.blockCenter.z + rotatedZ
        );
    }

    public static class Tintable extends DisplayItemBlockEntityController implements TintSource {

        public Tintable(BlockEntity blockEntity, DisplayItemBlockBehavior behavior) {
            super(blockEntity, behavior);
        }

        @Override
        public Item tintSource() {
            return super.displayItem();
        }

        @Override
        public Item takeDisplayItem() {
            Item item = super.takeDisplayItem();
            super.blockEntity.updateConstantRenderers();
            return item;
        }

        @Override
        public void putDisplayItem(Item inputItem) {
            super.putDisplayItem(inputItem);
            super.blockEntity.updateConstantRenderers();
        }
    }
}
