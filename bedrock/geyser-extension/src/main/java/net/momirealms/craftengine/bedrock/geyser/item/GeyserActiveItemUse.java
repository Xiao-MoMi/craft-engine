package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserBedrockPackets;
import org.cloudburstmc.protocol.bedrock.data.inventory.HandSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction.TriggerType;
import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.function.Predicate;

public final class GeyserActiveItemUse {
    private final GeyserSession session;
    private final Predicate<String> registeredItem;
    private Hand hand;
    private int slot;
    private int netId;
    private String identifier;

    public GeyserActiveItemUse(GeyserSession session, Predicate<String> registeredItem) {
        this.session = session;
        this.registeredItem = registeredItem;
    }

    public void listen(GeyserBedrockPackets network) {
        network.onSend(ChangeDimensionPacket.class, event -> this.hand = null);
        network.onSend(RespawnPacket.class, event -> this.hand = null);
        network.onReceive(InventoryTransactionPacket.class, event -> {
            if (this.suppress(event.packet())) {
                event.setCancelled(true);
            }
        });
    }

    void update(byte flags) {
        this.hand = null;
        if ((flags & 1) == 0) {
            return;
        }
        // Java's living flags retain the active hand even when Geyser suppresses the Bedrock USING_ITEM flag.
        Hand active = (flags & 2) == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        PlayerInventory inventory = this.session.getPlayerInventory();
        GeyserItemStack stack = inventory.getItemInHand(active);
        if (!stack.isEmpty()) {
            String id = stack.getItemData(this.session).getDefinition().getIdentifier();
            if (this.registeredItem.test(id)) {
                this.slot = inventory.getHeldItemSlot();
                this.netId = stack.getNetId();
                this.identifier = id;
                this.hand = active;
            }
        }
    }

    boolean suppress(InventoryTransactionPacket packet) {
        if (this.hand == null || packet.getTransactionType() != InventoryTransactionType.ITEM_USE
                || packet.getActionType() != 0 || packet.getTriggerType() != TriggerType.SIMULATION_TICK) {
            return false;
        }
        Hand transactionHand = packet.getHand() == HandSlot.OFFHAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (this.hand != transactionHand) {
            return false;
        }
        PlayerInventory inventory = this.session.getPlayerInventory();
        if ((this.hand == Hand.MAIN_HAND && this.slot != inventory.getHeldItemSlot()) || inventory.getItemInHand(this.hand).getNetId() != this.netId) {
            this.hand = null;
            return false;
        }
        ItemData item = packet.getItemInHand();
        return (this.hand == Hand.OFF_HAND || packet.getHotbarSlot() == this.slot)
                && item != null && item.getDefinition() != null && this.identifier.equals(item.getDefinition().getIdentifier())
                && (!item.isUsingNetId() || item.getNetId() == this.netId);
    }
}
