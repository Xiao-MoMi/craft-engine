package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GeyserSelectedItemHandler {
    private final GeyserPlayer player;
    private final GeyserSession session;
    private ScheduledFuture<?> task;
    private final GeyserItemRuntime runtime;
    private final GeyserHeadItemViews heads;
    private final Map<Integer, Replacement> originals = new HashMap<>();
    private int selectedSlot;
    private String dimension;

    GeyserSelectedItemHandler(GeyserPlayer player, GeyserItemRuntime runtime, GeyserHeadItemViews heads) {
        this.player = player;
        this.session = player.session();
        this.runtime = runtime;
        this.heads = heads;
        this.selectedSlot = this.session.getPlayerInventory().getHeldItemSlot();
    }

    static GeyserSelectedItemHandler install(GeyserPlayer player, GeyserItemRuntime runtime, GeyserHeadItemViews heads, boolean pollSelection) {
        GeyserSelectedItemHandler handler = new GeyserSelectedItemHandler(player, runtime, heads);
        // 匹配与槽位读取均在 Geyser 事件循环执行，不能在网络线程临时修改匹配上下文。
        player.network().onReceive(MobEquipmentPacket.class, handler::restoreEquipment);
        player.network().onReceive(InventoryTransactionPacket.class, handler::restoreTransaction);
        player.network().onReceive(PlayerAuthInputPacket.class, handler::restorePlayerInput);
        player.network().onSend(InventoryContentPacket.class, handler::onInventoryContent);
        player.network().onSend(InventorySlotPacket.class, handler::onInventorySlot);
        player.network().onSend(MobArmorEquipmentPacket.class, handler::onArmorEquipment);
        player.network().onSend(MobEquipmentPacket.class, handler::onEquipment);
        player.network().onSend(ItemStackResponsePacket.class, handler::onStackResponse);
        if (pollSelection) {
            player.session().executeInEventLoop(handler::poll);
        }
        return handler;
    }

    public void close() {
        if (this.task != null) {
            this.task.cancel(false);
        }
        this.originals.clear();
    }

    private void poll() {
        if (this.player.isClosed()) {
            return;
        }
        if (this.session.isSpawned()) {
            String world = this.runtime.dimension();
            int selected = this.session.getPlayerInventory().getHeldItemSlot();
            if (!Objects.equals(this.dimension, world)) {
                this.dimension = world;
                this.selectedSlot = selected;
                this.session.getPlayerInventoryHolder().updateInventory();
                if (this.session.getInventoryHolder() != null) {
                    this.session.getInventoryHolder().updateInventory();
                }
            } else if (this.selectedSlot != selected) {
                int previous = this.selectedSlot;
                this.selectedSlot = selected;
                this.refresh(previous);
            }
        }
        // 同一维度类型下切换世界也要刷新；仅状态变化时发包，不定时重发整个背包。
        this.task = this.session.scheduleInEventLoop(this::poll, 50, TimeUnit.MILLISECONDS);
    }

    private void restoreEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        int slot = equipment.getHotbarSlot();
        int netId = 0;
        if (equipment.getContainerId() == ContainerId.INVENTORY && slot >= 0 && slot < 9) {
            netId = this.session.getPlayerInventory().getItem(slot + 36).getNetId();
        }
        equipment.setItem(this.restore(equipment.getItem(), netId));
    }

    private void restoreTransaction(GeyserPacketEvent<InventoryTransactionPacket> event) {
        InventoryTransactionPacket transaction = event.packet();
        transaction.setItemInHand(this.restore(transaction.getItemInHand(), this.session.getPlayerInventory().getItemInHand().getNetId()));
        this.restoreActions(transaction.getActions());
    }

    private void restorePlayerInput(GeyserPacketEvent<PlayerAuthInputPacket> event) {
        PlayerAuthInputPacket input = event.packet();
        ItemUseTransaction transaction = input.getItemUseTransaction();
        if (transaction != null) {
            transaction.setItemInHand(this.restore(transaction.getItemInHand(), this.session.getPlayerInventory().getItemInHand().getNetId()));
            this.restoreActions(transaction.getActions());
        }
    }

    private void restoreActions(List<InventoryActionData> actions) {
        for (int i = 0; i < actions.size(); i++) {
            InventoryActionData action = actions.get(i);
            ItemData from = this.restore(action.getFromItem());
            ItemData to = this.restore(action.getToItem());
            if (from != action.getFromItem() || to != action.getToItem()) {
                actions.set(i, new InventoryActionData(action.getSource(), action.getSlot(), from, to, action.getStackNetworkId()));
            }
        }
    }

    private void onInventoryContent(GeyserPacketEvent<InventoryContentPacket> event) {
        InventoryContentPacket inventory = event.packet();
        this.renderInventory(inventory);
    }

    private void onInventorySlot(GeyserPacketEvent<InventorySlotPacket> event) {
        InventorySlotPacket inventory = event.packet();
        inventory.setItem(this.renderSlot(inventory.getItem(), inventory.getContainerId(), inventory.getSlot()));
    }

    private void onArmorEquipment(GeyserPacketEvent<MobArmorEquipmentPacket> event) {
        MobArmorEquipmentPacket armor = event.packet();
        armor.setHelmet(this.heads.equipped(armor.getHelmet()));
    }

    private void onEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        boolean own = equipment.getRuntimeEntityId() == this.session.getPlayerEntity().geyserId();
        boolean mainHand = equipment.getContainerId() == ContainerId.INVENTORY;
        equipment.setItem(this.render(equipment.getItem(), own && mainHand && equipment.getHotbarSlot() == this.session.getPlayerInventory().getHeldItemSlot()));
    }

    private void onStackResponse(GeyserPacketEvent<ItemStackResponsePacket> event) {
        this.refresh(-1);
    }

    private void renderInventory(InventoryContentPacket inventory) {
        List<ItemData> contents = inventory.getContents();
        List<ItemData> changed = null;
        for (int i = 0; i < contents.size(); i++) {
            ItemData item = this.renderSlot(contents.get(i), inventory.getContainerId(), i);
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

    private ItemData renderSlot(ItemData item, int container, int slot) {
        if (container == ContainerId.ARMOR && slot == 0) {
            return this.heads.equipped(item);
        }
        return this.render(item, container == ContainerId.INVENTORY && slot == this.session.getPlayerInventory().getHeldItemSlot());
    }

    private void refresh(int previous) {
        this.session.executeInEventLoop(() -> {
            if (this.player.isClosed()) {
                return;
            }
            PlayerInventory inventory = this.session.getPlayerInventory();
            Set<Integer> active = new HashSet<>();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                active.add(inventory.getItem(slot).getNetId());
            }
            if (this.session.getOpenInventory() != null) {
                for (int slot = 0; slot < this.session.getOpenInventory().getSize(); slot++) {
                    active.add(this.session.getOpenInventory().getItem(slot).getNetId());
                }
            }
            active.add(inventory.getCursor().getNetId());
            this.originals.keySet().retainAll(active);
            int selected = inventory.getHeldItemSlot();
            for (int slot = 0; slot < 36; slot++) {
                if (previous >= 0 && slot != previous && slot != selected) {
                    continue;
                }
                int javaSlot = slot < 9 ? slot + 36 : slot;
                ItemData item = inventory.getItem(javaSlot).getItemData(this.session);
                if (slot == previous || slot == selected || this.runtime.hasSelected(item)) {
                    InventorySlotPacket update = new InventorySlotPacket();
                    update.setContainerId(ContainerId.INVENTORY);
                    update.setSlot(slot);
                    update.setItem(item);
                    this.session.sendUpstreamPacket(update);
                }
            }
        });
    }

    @Nullable
    ItemData render(@Nullable ItemData item, boolean selected) {
        if (item == null || !selected || !this.runtime.hasSelected(item)) {
            return item;
        }
        GeyserItemStack stack = this.session.getPlayerInventory().getItemInHand();
        // 排队中的旧槽位包不能使用新物品的组件；等待对应的最新槽位更新。
        if (stack.getNetId() != item.getNetId()) {
            return item;
        }
        ItemData target = this.runtime.selected(stack);
        Render display = new Render(target.getDefinition(), target.getBlockDefinition());
        this.originals.put(item.getNetId(), new Replacement(new Render(item.getDefinition(), item.getBlockDefinition()), display));
        return replace(item, display);
    }

    @Nullable
    ItemData restore(@Nullable ItemData item) {
        return this.restore(item, 0);
    }

    @Nullable
    ItemData restore(@Nullable ItemData item, int slotNetId) {
        item = this.heads.unequipped(item);
        if (item == null || !this.runtime.hasSelected(item)) {
            return item;
        }
        Replacement replacement = this.originals.get(item.getNetId() > 0 ? item.getNetId() : slotNetId);
        if (replacement == null || item.getDefinition() != replacement.display().item()) {
            return item;
        }
        return replace(item, replacement.original());
    }

    private static ItemData replace(ItemData item, Render target) {
        if (item.getDefinition() == target.item() && item.getBlockDefinition() == target.block()) {
            return item;
        }
        return item.toBuilder().definition(target.item()).blockDefinition(target.block()).build();
    }

    record Render(ItemDefinition item, @Nullable BlockDefinition block) {
    }

    record Replacement(Render original, Render display) {
    }
}
