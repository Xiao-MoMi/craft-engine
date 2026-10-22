package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.BlockMappingsProxy;
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
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GeyserSelectedItemHandler extends ChannelDuplexHandler {
    private static final String HANDLER_NAME = "craftengine-selected-items";
    private final GeyserSession session;
    private final Map<String, Pair> variants;
    private int selectedSlot;

    GeyserSelectedItemHandler(GeyserSession session, Map<String, Pair> variants) {
        this.session = session;
        this.variants = variants;
        this.selectedSlot = session.getPlayerInventory().getHeldItemSlot();
    }

    public static void install(GeyserSession session, List<Variants> items) {
        Map<String, BlockDefinition> blocks = new HashMap<>();
        // Spigot 会重定位 fastutil，通过 proxy 返回 Map，避免依赖被重定位的返回类型。
        Map<CustomBlockState, BlockDefinition> states = BlockMappingsProxy.INSTANCE.getCustomBlockStateDefinitions(session.getBlockMappings());
        for (Map.Entry<CustomBlockState, BlockDefinition> entry : states.entrySet()) {
            blocks.put(entry.getKey().block().identifier(), entry.getValue());
        }
        Map<String, Pair> variants = new HashMap<>();
        for (Variants item : items) {
            Pair pair = new Pair(resolve(session, blocks, item.selected()), resolve(session, blocks, item.unselected()));
            variants.put(item.selected().item(), pair);
            variants.put(item.unselected().item(), pair);
        }
        GeyserSelectedItemHandler handler = new GeyserSelectedItemHandler(session, variants);
        Channel channel = session.getUpstream().getSession().getPeer().getChannel();
        channel.pipeline().addBefore(BedrockPeer.NAME, HANDLER_NAME, handler);
        handler.refresh(-1);
    }

    private static Render resolve(GeyserSession session, Map<String, BlockDefinition> blocks, Reference reference) {
        ItemDefinition item = session.getItemMappings().getDefinition(reference.item());
        BlockDefinition block = reference.block() == null ? null : blocks.get(reference.block());
        if (item == null || (reference.block() != null && block == null)) {
            throw new IllegalStateException("Missing selected item runtime mapping: " + reference.item());
        }
        return new Render(item, block);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        boolean selection = false;
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getSenderSubClientId() == 0) {
            selection = this.restoreIncoming(wrapper.getPacket());
        }
        super.channelRead(context, message);
        if (selection) {
            // 等 Geyser 验证并处理切槽后再读取槽位；不能用客户端上报的物品内容判定 selected。
            this.session.executeInEventLoop(() -> {
                int slot = this.session.getPlayerInventory().getHeldItemSlot();
                context.executor().execute(() -> this.select(slot));
            });
        }
    }

    private boolean restoreIncoming(BedrockPacket packet) {
        // Geyser 按未选中变体校验交互物品，回程必须还原显示替换，保留 netId 和物品数据。
        switch (packet) {
            case MobEquipmentPacket equipment -> {
                equipment.setItem(this.render(equipment.getItem(), false));
                return equipment.getContainerId() == ContainerId.INVENTORY;
            }
            case InventoryTransactionPacket transaction -> {
                transaction.setItemInHand(this.render(transaction.getItemInHand(), false));
                this.restoreActions(transaction.getActions());
                return true;
            }
            case PlayerAuthInputPacket input -> {
                ItemUseTransaction transaction = input.getItemUseTransaction();
                if (transaction != null) {
                    transaction.setItemInHand(this.render(transaction.getItemInHand(), false));
                    this.restoreActions(transaction.getActions());
                    return true;
                }
            }
            default -> {
            }
        }
        return false;
    }

    private void restoreActions(List<InventoryActionData> actions) {
        for (int i = 0; i < actions.size(); i++) {
            InventoryActionData action = actions.get(i);
            ItemData from = this.render(action.getFromItem(), false);
            ItemData to = this.render(action.getToItem(), false);
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
            case InventorySlotPacket inventory -> inventory.setItem(this.render(inventory.getItem(),
                    inventory.getContainerId() == ContainerId.INVENTORY && inventory.getSlot() == this.selectedSlot));
            case MobEquipmentPacket equipment -> {
                boolean own = equipment.getRuntimeEntityId() == this.session.getPlayerEntity().geyserId();
                boolean mainHand = equipment.getContainerId() == ContainerId.INVENTORY;
                equipment.setItem(this.render(equipment.getItem(), own && mainHand && equipment.getHotbarSlot() == this.selectedSlot));
            }
            case PlayerHotbarPacket hotbar -> {
                if (hotbar.getContainerId() == ContainerId.INVENTORY && hotbar.isSelectHotbarSlot()) {
                    this.select(hotbar.getSelectedHotbarSlot());
                }
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
            ItemData item = this.render(contents.get(i), inventory.getContainerId() == ContainerId.INVENTORY && i == this.selectedSlot);
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

    private void select(int slot) {
        if (slot >= 0 && slot < 9 && this.selectedSlot != slot) {
            int previous = this.selectedSlot;
            this.selectedSlot = slot;
            this.refresh(previous);
        }
    }

    private void refresh(int previous) {
        this.session.executeInEventLoop(() -> {
            PlayerInventory inventory = this.session.getPlayerInventory();
            int selected = inventory.getHeldItemSlot();
            for (int slot = 0; slot < 36; slot++) {
                if (previous >= 0 && slot != previous && slot != selected) {
                    continue;
                }
                int javaSlot = slot < 9 ? slot + 36 : slot;
                ItemData item = inventory.getItem(javaSlot).getItemData(this.session);
                if (slot == previous || slot == selected || this.variants.containsKey(item.getDefinition().getIdentifier())) {
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
        if (item == null) {
            return null;
        }
        Pair pair = this.variants.get(item.getDefinition().getIdentifier());
        if (pair == null) {
            return item;
        }
        Render target = selected ? pair.selected() : pair.unselected();
        if (item.getDefinition() == target.item() && item.getBlockDefinition() == target.block()) {
            return item;
        }
        return item.toBuilder().definition(target.item()).blockDefinition(target.block()).build();
    }

    public record Reference(String item, @Nullable String block) {
    }

    public record Variants(Reference selected, Reference unselected) {
    }

    record Render(ItemDefinition item, @Nullable BlockDefinition block) {
    }

    record Pair(Render selected, Render unselected) {
    }
}
