package net.momirealms.craftengine.bedrock.geyser.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GeyserSelectedItemHandler {
    private static final int PRUNE_INTERVAL = 32;
    private final GeyserPlayer player;
    private final GeyserSession session;
    private ScheduledFuture<?> task;
    private InventoryRefresh pending;
    private IntOpenHashSet liveNetIds;
    private int transactionsUntilPrune = PRUNE_INTERVAL;
    private final GeyserItemRuntime runtime;
    private final GeyserHeadItemViews heads;
    private final Int2ObjectOpenHashMap<Replacement> originals = new Int2ObjectOpenHashMap<>();
    private int selectedSlot;
    private String dimension;

    GeyserSelectedItemHandler(GeyserPlayer player, GeyserItemRuntime runtime, GeyserHeadItemViews heads) {
        this.player = player;
        this.session = player.session();
        this.runtime = runtime;
        this.heads = heads;
        this.selectedSlot = this.session.getPlayerInventory().getHeldItemSlot();
    }

    static GeyserSelectedItemHandler install(GeyserPlayer player, GeyserItemRuntime runtime, GeyserHeadItemViews heads, boolean usesDimension) {
        GeyserSelectedItemHandler handler = new GeyserSelectedItemHandler(player, runtime, heads);
        // 匹配与槽位读取均在 Geyser 事件循环执行，不能在网络线程临时修改匹配上下文。
        if (usesDimension && player.session().isSpawned()) {
            player.session().executeInEventLoop(handler::onWorldChanged);
        }
        return handler;
    }

    public void close() {
        if (this.task != null) {
            this.task.cancel(false);
        }
        this.originals.clear();
    }

    public void onWorldChanged() {
        if (this.player.isClosed()) {
            return;
        }
        if (!this.session.isSpawned()) {
            // Login/respawn has committed the world ID but the client may still be inside its loading screen.
            if (this.task == null) {
                this.task = this.session.scheduleInEventLoop(() -> {
                    this.task = null;
                    this.onWorldChanged();
                }, 1, TimeUnit.SECONDS);
            }
            return;
        }
        if (this.task != null) {
            this.task.cancel(false);
            this.task = null;
        }
        String world = this.runtime.dimension();
        if (!Objects.equals(this.dimension, world)) {
            this.dimension = world;
            this.dirtySlots().full = true;
        }
    }

    public void onSelectedSlotChanged() {
        if (!this.player.isClosed() && this.runtime.usesSelection()) {
            PlayerInventory inventory = this.session.getPlayerInventory();
            int selected = inventory.getHeldItemSlot();
            if (selected != this.selectedSlot) {
                InventoryRefresh slots = this.dirtySlots();
                slots.player |= 1L << inventory.getOffsetForHotbar(this.selectedSlot);
                slots.player |= 1L << inventory.getOffsetForHotbar(selected);
                this.selectedSlot = selected;
            }
        }
    }

    public void restoreEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        int slot = equipment.getHotbarSlot();
        int netId = 0;
        if (equipment.getContainerId() == ContainerId.INVENTORY && slot >= 0 && slot < 9) {
            netId = this.session.getPlayerInventory().getItem(slot + 36).getNetId();
        }
        equipment.setItem(this.restore(equipment.getItem(), netId));
    }

    public void restoreTransaction(GeyserPacketEvent<InventoryTransactionPacket> event) {
        InventoryTransactionPacket transaction = event.packet();
        transaction.setItemInHand(this.restore(transaction.getItemInHand(), this.session.getPlayerInventory().getItemInHand().getNetId()));
        this.restoreActions(transaction.getActions());
    }

    public void restorePlayerInput(GeyserPacketEvent<PlayerAuthInputPacket> event) {
        PlayerAuthInputPacket input = event.packet();
        ItemUseTransaction transaction = input.getItemUseTransaction();
        if (transaction != null) {
            transaction.setItemInHand(this.restore(transaction.getItemInHand(), this.session.getPlayerInventory().getItemInHand().getNetId()));
            this.restoreActions(transaction.getActions());
        }
    }

    private void restoreActions(List<InventoryActionData> actions) {
        StackIndex index = !actions.isEmpty() && !this.originals.isEmpty() ? new StackIndex() : null;
        for (int i = 0; i < actions.size(); i++) {
            InventoryActionData action = actions.get(i);
            ItemData from = this.restore(action.getFromItem(), 0, index);
            ItemData to = this.restore(action.getToItem(), 0, index);
            if (from != action.getFromItem() || to != action.getToItem()) {
                actions.set(i, new InventoryActionData(action.getSource(), action.getSlot(), from, to, action.getStackNetworkId()));
            }
        }
    }

    public void onInventoryContent(GeyserPacketEvent<InventoryContentPacket> event) {
        InventoryContentPacket inventory = event.packet();
        this.renderInventory(inventory);
    }

    public void onInventorySlot(GeyserPacketEvent<InventorySlotPacket> event) {
        InventorySlotPacket inventory = event.packet();
        inventory.setItem(this.renderSlot(inventory.getItem(), inventory.getContainerId(), inventory.getSlot(), null));
    }

    public void onArmorEquipment(GeyserPacketEvent<MobArmorEquipmentPacket> event) {
        MobArmorEquipmentPacket armor = event.packet();
        armor.setHelmet(this.heads.equipped(armor.getHelmet()));
    }

    public void onEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        boolean own = equipment.getRuntimeEntityId() == this.session.getPlayerEntity().geyserId();
        boolean mainHand = equipment.getContainerId() == ContainerId.INVENTORY;
        GeyserItemStack stack;
        if (own) {
            PlayerInventory inventory = this.session.getPlayerInventory();
            if (mainHand) {
                stack = inventory.getItemInHand();
            } else {
                stack = inventory.getOffhand();
            }
        } else {
            Entity entity = this.session.getEntityCache().getEntityByGeyserId(equipment.getRuntimeEntityId());
            if (!(entity instanceof LivingEntity living)) {
                return;
            }
            if (mainHand) {
                stack = living.getMainHandItem();
            } else {
                stack = living.getOffHandItem();
            }
        }
        if (stack != null) {
            equipment.setItem(this.render(equipment.getItem(), own && mainHand, stack, -1, null));
        }
    }

    public void onAddPlayer(GeyserPacketEvent<AddPlayerPacket> event) {
        AddPlayerPacket packet = event.packet();
        Entity entity = this.session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        if (entity instanceof LivingEntity living) {
            packet.setHand(this.render(packet.getHand(), false, living.getMainHandItem(), -1, null));
        }
    }

    public void onStackResponse(GeyserPacketEvent<ItemStackResponsePacket> event) {
        this.dirtySlots().add(event.packet(), this.session.getInventoryHolder());
    }

    private void renderInventory(InventoryContentPacket inventory) {
        List<ItemData> contents = inventory.getContents();
        List<ItemData> changed = null;
        StackIndex index = contents.size() > 1 && this.runtime.hasTints() ? new StackIndex() : null;
        for (int i = 0; i < contents.size(); i++) {
            ItemData item = this.renderSlot(contents.get(i), inventory.getContainerId(), i, index);
            if (item != contents.get(i)) {
                if (changed == null) {
                    changed = new ArrayList<>(contents);
                }
                changed.set(i, item);
            }
        }
        if (changed != null) {
            inventory.setContents(changed);
        }
    }

    private ItemData renderSlot(ItemData item, int container, int slot, @Nullable StackIndex index) {
        if (container == ContainerId.ARMOR && slot == 0) {
            return this.heads.equipped(item);
        }
        PlayerInventory inventory = this.session.getPlayerInventory();
        int playerSlot = -1;
        if (container == ContainerId.INVENTORY && slot >= 0 && slot < 36) {
            playerSlot = slot < 9 ? inventory.getOffsetForHotbar(slot) : slot;
        } else if (container == ContainerId.OFFHAND && slot == 0) {
            playerSlot = 45;
        }
        return this.render(item, container == ContainerId.INVENTORY && slot == inventory.getHeldItemSlot(), null, playerSlot, index);
    }

    private InventoryRefresh dirtySlots() {
        if (this.pending == null) {
            this.pending = new InventoryRefresh();
            this.session.executeInEventLoop(this::flushSlots);
        }
        return this.pending;
    }

    private void flushSlots() {
        InventoryRefresh slots = this.pending;
        this.pending = null;
        if (slots == null || this.player.isClosed()) {
            return;
        }
        if (!this.originals.isEmpty() && (slots.full || this.overCapacity() || (slots.prune && --this.transactionsUntilPrune <= 0))) {
            this.transactionsUntilPrune = PRUNE_INTERVAL;
            if (this.liveNetIds == null) {
                this.liveNetIds = new IntOpenHashSet();
            }
            this.liveNetIds.clear();
            PlayerInventory inventory = this.session.getPlayerInventory();
            this.collectIds(inventory);
            this.collectIds(this.session.getOpenInventory());
            this.liveNetIds.add(inventory.getCursor().getNetId());
            this.originals.keySet().retainAll(this.liveNetIds);
        }
        InventoryHolder<?> open = this.session.getInventoryHolder();
        if (slots.full || (slots.holder != null && slots.holder != open)) {
            this.session.getPlayerInventoryHolder().updateInventory();
            InventoryUtils.updateCursor(this.session);
            if (open != null) {
                open.updateInventory();
            }
            return;
        }
        for (long mask = slots.player; mask != 0; mask &= mask - 1) {
            this.session.getPlayerInventoryHolder().updateSlot(Long.numberOfTrailingZeros(mask));
        }
        if (slots.cursor) {
            InventoryUtils.updateCursor(this.session);
        }
        if (slots.open != null) {
            for (int slot = slots.open.nextSetBit(0); slot >= 0; slot = slots.open.nextSetBit(slot + 1)) {
                open.updateSlot(slot);
            }
        }
    }

    private boolean overCapacity() {
        Inventory open = this.session.getOpenInventory();
        return this.originals.size() > this.session.getPlayerInventory().getSize() + (open == null ? 0 : open.getSize()) + 1 + PRUNE_INTERVAL;
    }

    private void collectIds(@Nullable Inventory inventory) {
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                this.liveNetIds.add(inventory.getItem(slot).getNetId());
            }
        }
    }

    private static final class StackIndex {
        private Int2ObjectOpenHashMap<GeyserItemStack> stacks;

        private void add(@Nullable Inventory inventory) {
            if (inventory != null) {
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    GeyserItemStack stack = inventory.getItem(slot);
                    if (!stack.isEmpty()) {
                        this.stacks.putIfAbsent(stack.getNetId(), stack);
                    }
                }
            }
        }
    }

    private GeyserItemStack findStack(int netId, int playerSlot, @Nullable StackIndex index) {
        if (netId <= 0) {
            return null;
        }
        PlayerInventory inventory = this.session.getPlayerInventory();
        if (playerSlot >= 0) {
            GeyserItemStack candidate = inventory.getItem(playerSlot);
            if (!candidate.isEmpty() && candidate.getNetId() == netId) {
                return candidate;
            }
        }
        if (inventory.getCursor().getNetId() == netId) {
            return inventory.getCursor();
        }
        if (index != null) {
            if (index.stacks == null) {
                Inventory open = this.session.getOpenInventory();
                index.stacks = new Int2ObjectOpenHashMap<>(inventory.getSize() + (open == null ? 0 : open.getSize()));
                index.add(inventory);
                index.add(open);
            }
            return index.stacks.get(netId);
        }
        GeyserItemStack stack = findStack(inventory, netId);
        return stack != null ? stack : findStack(this.session.getOpenInventory(), netId);
    }

    private static GeyserItemStack findStack(Inventory inventory, int netId) {
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                GeyserItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && stack.getNetId() == netId) {
                    return stack;
                }
            }
        }
        return null;
    }

    private ItemData render(ItemData item, boolean selected, @Nullable GeyserItemStack stack, int playerSlot, @Nullable StackIndex index) {
        if (item == null) {
            return null;
        }
        boolean useSelected = selected && this.runtime.hasSelected(item);
        GeyserItemTints tints = this.runtime.tints(item);
        if (!useSelected && tints == GeyserItemTints.DEFAULT) {
            return item;
        }
        if (stack == null) {
            stack = this.findStack(item.getNetId(), playerSlot, index);
        }
        if (stack == null || stack.isEmpty()) {
            return item;
        }
        // 队列中的旧物品包不能读取新物品的颜色。保留 Geyser 原有的组件校验标记。
        NbtMap tag = item.getTag();
        DataComponents components = null;
        if (tag != null && tag.containsKey("GeyserHash")) {
            components = stack.getAllComponents();
            if (tag.getInt("GeyserHash") != components.hashCode()) {
                return item;
            }
        }
        ItemData target = item;
        if (useSelected) {
            ItemData selectedItem = this.runtime.selected(stack);
            if (selectedItem.getDefinition() != item.getDefinition()) {
                tints = this.runtime.tints(selectedItem);
            }
            if (selectedItem.getDefinition() != item.getDefinition() || selectedItem.getBlockDefinition() != item.getBlockDefinition()) {
                target = item.toBuilder().definition(selectedItem.getDefinition()).blockDefinition(selectedItem.getBlockDefinition()).build();
            }
        }
        target = tints.render(components == null ? stack::getComponent : components::get, target);
        if (target != item && item.getNetId() > 0) {
            this.originals.put(item.getNetId(), new Replacement(Render.of(item), Render.of(target)));
            if (this.pending == null && this.overCapacity()) {
                this.dirtySlots();
            }
        } else if (useSelected && item.getNetId() > 0) {
            this.originals.remove(item.getNetId());
        }
        return target;
    }

    @Nullable
    ItemData restore(@Nullable ItemData item) {
        return this.restore(item, 0);
    }

    @Nullable
    ItemData restore(@Nullable ItemData item, int slotNetId) {
        return this.restore(item, slotNetId, null);
    }

    @Nullable
    private ItemData restore(@Nullable ItemData item, int slotNetId, @Nullable StackIndex index) {
        item = this.heads.unequipped(item);
        if (item == null || (!this.runtime.hasSelected(item) && this.runtime.tints(item) == GeyserItemTints.DEFAULT)) {
            return item;
        }
        int netId = item.getNetId() > 0 ? item.getNetId() : slotNetId;
        Replacement replacement = this.originals.get(netId);
        if (replacement != null && netId != slotNetId && this.findStack(netId, -1, index) == null) {
            this.originals.remove(netId);
            replacement = null;
        }
        if (replacement == null && item.getNetId() <= 0) {
            // A delayed sweep can retain a former stack; only a live stack can supply a missing network ID.
            StackIndex live = index == null ? new StackIndex() : index;
            for (Int2ObjectMap.Entry<Replacement> entry : this.originals.int2ObjectEntrySet()) {
                Replacement candidate = entry.getValue();
                if (item.getDefinition() == candidate.display().item() && Objects.equals(item.getTag(), candidate.display().tag())
                        && this.findStack(entry.getIntKey(), -1, live) != null) {
                    replacement = candidate;
                    break;
                }
            }
        }
        if (replacement == null || item.getDefinition() != replacement.display().item() || !Objects.equals(item.getTag(), replacement.display().tag())) {
            return item;
        }
        return replace(item, replacement.original());
    }

    private static ItemData replace(ItemData item, Render target) {
        if (item.getDefinition() == target.item() && item.getBlockDefinition() == target.block() && Objects.equals(item.getTag(), target.tag())) {
            return item;
        }
        return item.toBuilder().definition(target.item()).blockDefinition(target.block()).tag(target.tag()).build();
    }

    record Render(ItemDefinition item, @Nullable BlockDefinition block, @Nullable NbtMap tag) {
        static Render of(ItemData item) {
            return new Render(item.getDefinition(), item.getBlockDefinition(), item.getTag());
        }
    }

    record Replacement(Render original, Render display) {
    }
}
