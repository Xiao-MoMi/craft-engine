package net.momirealms.craftengine.bedrock.geyser.item;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GeyserSelectedItemHandler extends ChannelDuplexHandler {
    private static final String HANDLER_NAME = "craftengine-selected-items";
    private final GeyserSession session;
    private final GeyserItemRuntime runtime;
    private final Map<Integer, Replacement> originals = new HashMap<>();
    private int selectedSlot;
    private String dimension;

    GeyserSelectedItemHandler(GeyserSession session, GeyserItemRuntime runtime) {
        this.session = session;
        this.runtime = runtime;
        this.selectedSlot = session.getPlayerInventory().getHeldItemSlot();
    }

    public static void install(GeyserSession session, GeyserItemRuntime runtime) {
        GeyserSelectedItemHandler handler = new GeyserSelectedItemHandler(session, runtime);
        Channel channel = session.getUpstream().getSession().getPeer().getChannel();
        // 匹配与槽位读取均在 Geyser 事件循环执行，不能在网络线程临时修改匹配上下文。
        channel.pipeline().addBefore(session.getTickEventLoop(), BedrockPeer.NAME, HANDLER_NAME, handler);
        session.executeInEventLoop(handler::poll);
    }

    private void poll() {
        if (this.session.isClosed()) {
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
        this.session.scheduleInEventLoop(this::poll, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getSenderSubClientId() == 0) {
            this.restoreIncoming(wrapper.getPacket());
        }
        super.channelRead(context, message);
    }

    private void restoreIncoming(BedrockPacket packet) {
        // Geyser 按未选中变体校验交互物品，回程必须还原显示替换，保留 netId 和物品数据。
        switch (packet) {
            case MobEquipmentPacket equipment -> {
                int slot = equipment.getHotbarSlot();
                int netId = 0;
                if (equipment.getContainerId() == ContainerId.INVENTORY && slot >= 0 && slot < 9) {
                    netId = this.session.getPlayerInventory().getItem(slot + 36).getNetId();
                }
                equipment.setItem(this.restore(equipment.getItem(), netId));
            }
            case InventoryTransactionPacket transaction -> {
                transaction.setItemInHand(this.restore(transaction.getItemInHand(), this.session.getPlayerInventory().getItemInHand().getNetId()));
                this.restoreActions(transaction.getActions());
            }
            case PlayerAuthInputPacket input -> {
                ItemUseTransaction transaction = input.getItemUseTransaction();
                if (transaction != null) {
                    transaction.setItemInHand(this.restore(transaction.getItemInHand(), this.session.getPlayerInventory().getItemInHand().getNetId()));
                    this.restoreActions(transaction.getActions());
                }
            }
            default -> {
            }
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

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getTargetSubClientId() == 0) {
            this.renderOutgoing(wrapper.getPacket());
        }
        super.write(context, message, promise);
    }

    private void renderOutgoing(BedrockPacket packet) {
        switch (packet) {
            case InventoryContentPacket inventory -> this.renderInventory(inventory);
            case InventorySlotPacket inventory -> inventory.setItem(this.render(inventory.getItem(), inventory.getContainerId() == ContainerId.INVENTORY && inventory.getSlot() == this.session.getPlayerInventory().getHeldItemSlot()));
            case MobEquipmentPacket equipment -> {
                boolean own = equipment.getRuntimeEntityId() == this.session.getPlayerEntity().geyserId();
                boolean mainHand = equipment.getContainerId() == ContainerId.INVENTORY;
                equipment.setItem(this.render(equipment.getItem(), own && mainHand && equipment.getHotbarSlot() == this.session.getPlayerInventory().getHeldItemSlot()));
            }
            case ItemStackResponsePacket ignored -> this.refresh(-1);
            default -> {
            }
        }
    }

    private void renderInventory(InventoryContentPacket inventory) {
        List<ItemData> contents = inventory.getContents();
        List<ItemData> changed = null;
        for (int i = 0; i < contents.size(); i++) {
            ItemData item = this.render(contents.get(i), inventory.getContainerId() == ContainerId.INVENTORY && i == this.session.getPlayerInventory().getHeldItemSlot());
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

    private void refresh(int previous) {
        this.session.executeInEventLoop(() -> {
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
