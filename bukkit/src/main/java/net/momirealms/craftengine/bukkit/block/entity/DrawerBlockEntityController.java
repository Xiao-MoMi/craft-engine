package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.DrawerBlockBehavior;
import net.momirealms.craftengine.bukkit.block.entity.renderer.dynamic.DynamicDrawerBlockEntityElement;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.world.BukkitContainer;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.WorldlyContainer;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.IntTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@SuppressWarnings("DuplicatedCode")
public sealed abstract class DrawerBlockEntityController extends BlockEntityController implements BukkitContainer, WorldlyContainer, InventoryHolder {
    public final DrawerBlockBehavior behavior;
    public final DynamicDrawerBlockEntityElement element;
    protected final Object container;
    protected final Inventory inventory;
    protected WorldPosition itemPosition;
    protected WorldPosition textPosition;
    protected float entityYRot;
    protected final Vector3f blockCenter;
    protected Item lastUpdateItem = Item.empty(); // 最后一次包发送的物品
    protected int lastUpdateCount = 0; // 最后一次包发送的物品数量
    protected UUID lastClickPlayer;
    protected long lastClickTime;

    protected DrawerBlockEntityController(BlockEntity blockEntity, DrawerBlockBehavior behavior) {
        super(blockEntity);
        this.behavior = behavior;
        this.blockCenter = new Vector3f((float) (blockEntity.pos.x + 0.5), (float) (blockEntity.pos.y + 0.5), (float) (blockEntity.pos.z + 0.5));
        this.itemPosition = this.calculateDisplayPosition(blockEntity.blockState, this.behavior.itemPosition);
        this.textPosition = this.calculateDisplayPosition(blockEntity.blockState, this.behavior.textPosition);
        this.entityYRot = this.calculateYRot(blockEntity.blockState);
        this.element = new DynamicDrawerBlockEntityElement(this, this.itemPosition, this.textPosition, this.entityYRot);
        this.container = CraftEngine.instance().platform().createContainer(this);
        this.inventory = CraftInventoryProxy.INSTANCE.newInstance(this.container);
    }

    public Object container() {
        return this.container;
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
    public abstract Item item();

    public abstract int itemCount();

    public abstract int maxCount();

    public abstract void clearItems();

    public abstract boolean isFull();

    /**
     * 放入物品，返回值为实际放入的量
     *
     * @param inputItem 不为空的物品
     * @param count 尝试放入的数量
     */
    public abstract int put(Item inputItem, int count);

    public abstract int add(int count);

    @SuppressWarnings("UnusedReturnValue")
    public abstract int take(int count, Consumer<Item> consumer, boolean update);

    // 方块状态变更时
    @Override
    public void preBlockStateChange(ImmutableBlockState newState) {
        this.itemPosition = this.calculateDisplayPosition(newState, this.behavior.itemPosition);
        this.textPosition = this.calculateDisplayPosition(newState, this.behavior.textPosition);
        this.entityYRot = this.calculateYRot(newState);
        this.refreshElementPosPacket();
        this.refreshDynamicElement(DynamicDrawerBlockEntityElement::updateElementPos);
    }

    @Override
    public abstract void loadCustomData(CompoundTag tag);

    @Override
    public abstract void saveCustomData(CompoundTag tag);

    @Override
    public abstract void onRemove();

    // 刷新展示元素
    public void refreshDynamicElement(BiConsumer<DynamicDrawerBlockEntityElement, Player> consumer) {
        CEChunk chunk = super.blockEntity.world.getChunkAtIfLoaded(super.blockEntity.pos.x >> 4, super.blockEntity.pos.z >> 4);
        if (chunk == null) return;
        for (Player trackedPlayer : chunk.getTrackedBy()) {
            consumer.accept(this.element, trackedPlayer);
        }
    }

    // 检查并刷新元素物品展示实体的内容包
    public boolean refreshItemDisplayPacket() {
        Item displayItem = this.item();
        boolean result = false;
        if (displayItem.minecraftItem() == this.lastUpdateItem.minecraftItem()) {
            return false;
        }
        if (!displayItem.isSimilar(this.lastUpdateItem)) {
            this.lastUpdateItem = displayItem;
            this.element.refreshChangeDisplayItemPacket(displayItem);
            result = true;
        }
        return result;
    }

    // 检查并刷新元素展示的文本实体的内容包
    public boolean refreshTextContentPacket() {
        int storageCount = this.itemCount();
        boolean result = false;
        if (this.lastUpdateCount != storageCount) {
            this.lastUpdateCount = storageCount;
            this.element.refreshChangeTextContentPacket(storageCount);
            result = true;
        }
        return result;
    }

    // 检查并刷新元素展示位置的内容包
    public void refreshElementPosPacket() {
        this.element.refreshSpawnItemAndTextPacket(this.itemPosition, this.textPosition, this.entityYRot);
    }

    public WorldPosition calculateDisplayPosition(ImmutableBlockState blockState, Vector3f relative) {
        float angleDeg;
        if (this.behavior.directionProperty != null) {
            Direction direction = blockState.get(this.behavior.directionProperty, Direction.SOUTH);
            switch (direction) {
                case EAST -> angleDeg = 90f;
                case SOUTH -> angleDeg = 180f;
                case WEST -> angleDeg = 270f;
                default -> angleDeg = 0f;
            }
        } else {
            angleDeg = 0f;
        }
        double angleRad = Math.toRadians(angleDeg);

        float x = -relative.x;
        float z = relative.z;
        double rotatedX = x * Math.cos(angleRad) - z * Math.sin(angleRad);
        double rotatedZ = x * Math.sin(angleRad) + z * Math.cos(angleRad);

        return new WorldPosition(null,
                this.blockCenter.x + rotatedX,
                this.blockCenter.y + relative.y,
                this.blockCenter.z + rotatedZ
        );
    }

    public float calculateYRot(ImmutableBlockState blockState) {
        if (this.behavior.directionProperty == null) return 0f;
        Direction direction = blockState.get(this.behavior.directionProperty, Direction.SOUTH);
        return switch (direction.opposite()) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default ->  0f;
        };
    }

    public UUID lastClickPlayer() {
        return lastClickPlayer;
    }

    public void lastClickPlayer(UUID lastClickPlayer) {
        this.lastClickPlayer = lastClickPlayer;
    }

    public long lastClickTime() {
        return lastClickTime;
    }

    public void lastClickTime(long lastClickTime) {
        this.lastClickTime = lastClickTime;
    }

    @Override
    public abstract int[] getSlotsForFace(Direction direction);

    @Override
    public abstract boolean canPlaceItemThroughFace(int slot, Item stack, Direction direction);

    @Override
    public abstract boolean canTakeItemThroughFace(int slot, Item stack, Direction direction);

    @Override
    public abstract int containerSize();

    @Override
    public abstract boolean isEmpty();

    @Override
    public abstract Item getItem(int slot);

    @Override
    public abstract Item removeItem(int slot, int count);

    @Override
    public abstract Item removeItemNoUpdate(int slot);

    @Override
    public abstract void setItem(int slot, Item item);

    @Override
    public abstract int maxStackSize();

    @Override
    public void setChanged() {
        boolean previousEmpty = this.lastUpdateItem.isEmpty();
        boolean isEmpty = this.isEmpty();
        boolean changedItem = this.refreshItemDisplayPacket();
        boolean changedCount = this.refreshTextContentPacket();

        if (isEmpty) {
            // 空了
            this.refreshDynamicElement(DynamicDrawerBlockEntityElement::hide);
        } else if (changedItem && changedCount) {
            // 都变了
            this.refreshDynamicElement((e, p) -> e.updateItemAndText(p, previousEmpty));
        } else if (changedItem) {
            // 只变了物品
            this.refreshDynamicElement(DynamicDrawerBlockEntityElement::updateDisplayItem);
        } else if (changedCount) {
            // 只变了数量
            this.refreshDynamicElement(DynamicDrawerBlockEntityElement::updateTextContent);
        }

        CEWorld ceWorld = blockEntity.world;
        if (ceWorld == null) return;
        ceWorld.blockEntityChanged(blockEntity.pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.clearItems();
        this.setChanged();
    }

    @Override
    public List<Item> contents() {
        return Collections.singletonList(this.item());
    }

    @Override
    public void setMaxStackSize(int size) {
    }

    @Override
    public WorldPosition position() {
        return new WorldPosition(
                super.blockEntity.world.world,
                super.blockEntity.pos.x,
                super.blockEntity.pos.y,
                super.blockEntity.pos.z
        );
    }

    @Override
    public void onOpen(HumanEntity player) {
    }

    @Override
    public void onClose(HumanEntity player) {
    }

    @Override
    public List<HumanEntity> getViewers() {
        return List.of();
    }

    @Override
    public InventoryHolder getOwner() {
        return this;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public static DrawerBlockEntityController create(BlockEntity blockEntity, DrawerBlockBehavior behavior) {
        return behavior.compatibleMode ? new Compatible(blockEntity, behavior) : new Modern(blockEntity, behavior);
    }

    private static final class Modern extends DrawerBlockEntityController {
        private static final int HOPPER_PLACE_SLOT = -1;
        private static final int HOPPER_TAKE_SLOT = -2;
        private static final int[] SLOTS = new int[]{HOPPER_PLACE_SLOT, HOPPER_TAKE_SLOT};
        private Item templateItem = Item.empty();
        private int itemCount = 0;
        private int maxCount = this.behavior.maxStacks; // this.behavior.maxStacks * 1

        private Modern(BlockEntity blockEntity, DrawerBlockBehavior behavior) {
            super(blockEntity, behavior);
        }

        @NotNull
        @Override
        public Item item() {
            return this.templateItem.copyWithCount(1);
        }

        private void setTemplateItem(@Nullable Item item) {
            if (item == null) {
                this.templateItem = Item.empty();
                this.maxCount = this.behavior.maxStacks; // this.behavior.maxStacks * 1
            } else {
                this.templateItem = item.copyWithCount(1);
                this.maxCount = this.behavior.maxStacks * item.maxStackSize();
            }
        }

        @Override
        public int itemCount() {
            return this.itemCount;
        }

        @Override
        public int maxCount() {
            return this.maxCount;
        }

        private void addItemCount(int count) {
            this.itemCount += count;
        }

        private void setItemCount(int count) {
            this.itemCount = count;
        }

        @Override
        public void clearItems() {
            this.setTemplateItem(null);
            this.setItemCount(0);
        }

        @Override
        public boolean isFull() {
            return this.itemCount() >= this.maxCount();
        }

        @Override
        public int put(Item inputItem, int count) {
            if (count <= 0 || inputItem.isEmpty()) return 0;

            if (isEmpty()) {
                this.setTemplateItem(inputItem);
            } else if (!this.templateItem.isSimilar(inputItem)) {
                return 0;
            }

            int actualAdded = Math.min(count, this.maxCount() - this.itemCount());
            this.addItemCount(actualAdded);

            this.setChanged();
            return actualAdded;
        }

        // 增加存储物品的数量，返回值为实际增加的量
        @Override
        public int add(int count) {
            if (count <= 0 || isEmpty()) return 0;
            int actualAdded = Math.min(count, this.maxCount() - this.itemCount());
            this.addItemCount(actualAdded);
            this.setChanged();
            return actualAdded;
        }

        // 取走方块内的物品，返回值为实际取走的量
        @Override
        public int take(int count, Consumer<Item> consumer, boolean update) {
            if (count <= 0 || isEmpty()) return 0;

            // 记录物品模板，用于后续生成 Item
            Item template = this.item();
            int actualTaken = Math.min(count, this.itemCount());

            if (actualTaken <= 0) return 0;

            this.addItemCount(-actualTaken);

            if (this.itemCount() <= 0) {
                this.clearItems();
            }

            int maxStack = template.maxStackSize();
            int toCallback = actualTaken;
            while (toCallback > 0) {
                int currentBatch = Math.min(toCallback, maxStack);
                toCallback -= currentBatch;
                consumer.accept(template.copyWithCount(currentBatch));
            }
            if (update) {
                this.setChanged();
            }

            return actualTaken;
        }

        // 读取方块内存储的物品
        @Override
        public void loadCustomData(CompoundTag tag) {
            CompoundTag dataTag = tag.getCompound(behavior.customDataKey);
            // 空数据
            if (dataTag == null) return;
            // 读取数据
            int dataVersion = dataTag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
            Tag itemTag = dataTag.get("item");
            int count = dataTag.getInt("count", 0);
            // 非法数据
            if (itemTag == null || count <= 0) return;

            this.setTemplateItem(ItemStackUtils.wrap(ItemStackUtils.parseMinecraftItem(itemTag, dataVersion)));
            this.setItemCount(count);

            Item item = this.item();
            this.element.refreshChangeDisplayItemPacket(item);
            this.element.refreshChangeTextContentPacket(count);
            this.lastUpdateItem = item;
            this.lastUpdateCount = count;
        }

        @Override
        public void saveCustomData(CompoundTag tag) {
            if (isEmpty() || this.itemCount() <= 0) return;
            CompoundTag data = new CompoundTag();
            data.put("data_version", new IntTag(Config.itemDataFixerUpperFallbackVersion()));
            data.put("count", new IntTag(this.itemCount()));
            data.put("item", ItemStackUtils.saveMinecraftItemStackAsTag(this.item().minecraftItem()));
            tag.put(behavior.customDataKey, data);
        }

        @Override
        public void onRemove() {
            if (this.itemCount() <= 0 || this.templateItem.isEmpty()) return;
            Item template = this.item();
            int count = this.itemCount();
            this.clearItems();
            int maxStackSize = template.maxStackSize();
            while (count > 0) {
                int toDrop = Math.min(count, maxStackSize);
                count -= toDrop;
                super.blockEntity.world.world().dropItemNaturally(this.itemPosition, template.copyWithCount(toDrop));
            }
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            return SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, Item stack, Direction direction) {
            return behavior.canPlaceItem
                    && slot == HOPPER_PLACE_SLOT
                    && (this.isEmpty() || this.templateItem.isSimilar(stack))
                    && this.itemCount() + stack.count() <= this.maxCount();
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, Item stack, Direction direction) {
            return behavior.canTakeItem && slot == HOPPER_TAKE_SLOT && !this.isEmpty();
        }

        @Override
        public int containerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.templateItem.isEmpty() && this.itemCount() <= 0;
        }

        @Override
        public Item getItem(int slot) {
            if (slot != HOPPER_TAKE_SLOT || this.isEmpty()) return Item.empty();
            return this.item();
        }

        @Override
        public Item removeItem(int slot, int count) {
            if (slot < 0) return Item.empty();
            Item item = this.item();
            take(count, $ -> {}, true);
            return item;
        }

        @Override
        public Item removeItemNoUpdate(int slot) {
            if (slot < 0) return Item.empty();
            Item item = this.item();
            take(this.itemCount(), $ -> {}, false);
            return item;
        }

        @Override
        public void setItem(int slot, Item item) {
            int count = item.count();
            if (slot == HOPPER_PLACE_SLOT && this.templateItem.isSimilar(item) && count == 1) {
                add(1); // 漏斗放入
            } else if (slot == HOPPER_TAKE_SLOT && item.isEmpty()) {
                take(1, $ -> {}, true); // 漏斗取出
            } else {
                this.setTemplateItem(item);
                this.setItemCount(count);
            }
        }

        @Override
        public int maxStackSize() {
            return 1;
        }
    }

    private static final class Compatible extends DrawerBlockEntityController {
        private final Item[] items;
        private final int[] slots;

        private Compatible(BlockEntity blockEntity, DrawerBlockBehavior behavior) {
            super(blockEntity, behavior);
            this.items = new Item[behavior.maxStacks];
            Arrays.fill(this.items, Item.empty());
            this.slots = IntStream.range(0, behavior.maxStacks).toArray();
        }

        @Override
        public @NotNull Item item() {
            return this.items[0];
        }

        @Override
        public int itemCount() {
            int count = 0;
            for (Item item : this.items) {
                if (item.isEmpty()) break;
                count += item.count();
            }
            return count;
        }

        @Override
        public int maxCount() {
            return this.behavior.maxStacks;
        }

        @Override
        public void clearItems() {
            Arrays.fill(this.items, Item.empty());
        }

        @Override
        public boolean isFull() {
            Item lastItem = this.items[this.items.length - 1];
            if (lastItem.isEmpty()) return false;
            return lastItem.count() >= lastItem.maxStackSize();
        }

        @Override
        public int put(Item inputItem, int count) {
            if (count <= 0) return 0;

            if (!isEmpty() && !this.items[0].isSimilar(inputItem)) {
                return 0;
            }

            int remainingToAdd = count;
            int maxStack = inputItem.maxStackSize();

            for (int i = 0; i < this.items.length && remainingToAdd > 0; i++) {
                if (this.items[i].isEmpty()) {
                    int toAdd = Math.min(remainingToAdd, maxStack);
                    this.items[i] = inputItem.copyWithCount(toAdd);
                    remainingToAdd -= toAdd;
                } else {
                    int currentCount = this.items[i].count();
                    int space = maxStack - currentCount;
                    if (space > 0) {
                        int toAdd = Math.min(remainingToAdd, space);
                        this.items[i].grow(toAdd);
                        remainingToAdd -= toAdd;
                    }
                }
            }

            this.setChanged();
            return count - remainingToAdd;
        }

        @Override
        public int add(int count) {
            if (!isEmpty() && count > 0) {
                Item baseItem = this.items[0];
                int maxStack = baseItem.maxStackSize();
                int remaining = count;

                for (int i = 0; i < this.items.length && remaining > 0; i++) {
                    if (this.items[i].isEmpty()) {
                        int toAdd = Math.min(remaining, maxStack);
                        this.items[i] = baseItem.copyWithCount(toAdd);
                        remaining -= toAdd;
                    } else {
                        int canAccept = maxStack - this.items[i].count();
                        int toAdd = Math.min(remaining, canAccept);
                        this.items[i].grow(toAdd);
                        remaining -= toAdd;
                    }
                }

                this.setChanged();
                return count - remaining;
            }
            return 0;
        }

        @Override
        public int take(int count, Consumer<Item> consumer, boolean update) {
            if (count <= 0 || isEmpty()) return 0;

            // 记录物品模板，用于后续生成 Item
            Item template = this.items[0].copyWithCount(1);
            int maxStack = template.maxStackSize();
            int remainingToTake = count;
            for (int i = this.items.length - 1; i >= 0 && remainingToTake > 0; i--) {
                if (this.items[i].isEmpty()) continue;

                int canTakeFromSlot = Math.min(remainingToTake, this.items[i].count());
                this.items[i].shrink(canTakeFromSlot);
                remainingToTake -= canTakeFromSlot;

                if (this.items[i].count() <= 0) {
                    this.items[i] = Item.empty();
                }
            }

            int actualTaken = count - remainingToTake;

            if (actualTaken > 0) {
                int toCallback = actualTaken;
                while (toCallback > 0) {
                    int currentBatch = Math.min(toCallback, maxStack);
                    consumer.accept(template.copyWithCount(currentBatch));
                    toCallback -= currentBatch;
                }
                this.setChanged();
            }

            return actualTaken;
        }

        @Override
        public void loadCustomData(CompoundTag tag) {
            CompoundTag dataTag = tag.getCompound(behavior.customDataKey);
            // 空数据
            if (dataTag == null) {
                return;
            }
            // 读取数据
            int dataVersion = dataTag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
            Tag itemTag = dataTag.get("item");
            int count = dataTag.getInt("count", 0);
            // 非法数据
            if (itemTag == null || count <= 0) {
                return;
            }

            Item itemTemplate = ItemStackUtils.wrap(ItemStackUtils.parseMinecraftItem(itemTag, dataVersion));
            int maxStackSize = itemTemplate.maxStackSize();
            int remaining = count;

            for (int i = 0; i < this.items.length; i++) {
                if (remaining <= 0) {
                    break;
                } else {
                    int currentCount = Math.min(remaining, maxStackSize);
                    this.items[i] = itemTemplate.copyWithCount(currentCount);
                    remaining -= currentCount;
                }
            }

            this.element.refreshChangeDisplayItemPacket(this.items[0]);
            this.element.refreshChangeTextContentPacket(count);
            this.lastUpdateItem = this.items[0].copy();
            this.lastUpdateCount = count;
        }

        @Override
        public void saveCustomData(CompoundTag tag) {
            if (isEmpty() || this.itemCount() <= 0) return;
            CompoundTag data = new CompoundTag();
            data.put("data_version", new IntTag(Config.itemDataFixerUpperFallbackVersion()));
            data.put("count", new IntTag(this.itemCount()));
            data.put("item", ItemStackUtils.saveMinecraftItemStackAsTag(this.items[0].copyWithCount(1).minecraftItem()));
            tag.put(behavior.customDataKey, data);
        }

        @Override
        public void onRemove() {
            for (Item item : this.items) {
                super.blockEntity.world.world().dropItemNaturally(this.itemPosition, item);
            }
            Arrays.fill(this.items, Item.empty());
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            return this.slots;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, Item stack, Direction direction) {
            return this.isEmpty() || stack.isSimilar(this.items[0]);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, Item stack, Direction direction) {
            if (slot < 0 || slot >= this.items.length || this.items[slot].isEmpty()) {
                return false;
            }
            // 保证漏斗从后往前遍历
            if (slot == this.items.length - 1) {
                return true;
            }
            return this.items[slot + 1].isEmpty();
        }

        @Override
        public int containerSize() {
            return this.behavior.maxStacks;
        }

        @Override
        public boolean isEmpty() {
            return this.items[0].isEmpty();
        }

        @Override
        public Item getItem(int slot) {
            return this.items[slot];
        }

        @Override
        public Item removeItem(int slot, int count) {
            Item item = this.items[slot];
            if (item.isEmpty()) return item;
            Item result;
            if (item.count() <= count) {
                this.setItem(slot, Item.empty());
                result = item;
            } else {
                result = item.copyWithCount(count);
                item.shrink(count);
            }
            this.setChanged();
            return result;
        }

        @Override
        public Item removeItemNoUpdate(int slot) {
            Item item = this.items[slot];
            if (item.isEmpty()) return item;
            Item result;
            if (item.count() <= 1) {
                this.setItem(slot, Item.empty());
                result = item;
            } else {
                result = item.copyWithCount(1);
                item.shrink(1);
            }
            return result;
        }

        @Override
        public void setItem(int slot, Item item) {
            this.items[slot] = item;
        }

        @Override
        public int maxStackSize() {
            return 99;
        }
    }
}
