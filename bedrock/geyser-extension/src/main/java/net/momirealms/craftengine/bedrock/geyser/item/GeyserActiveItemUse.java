package net.momirealms.craftengine.bedrock.geyser.item;

import org.cloudburstmc.protocol.bedrock.data.inventory.HandSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction.TriggerType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

import java.util.function.Predicate;

public final class GeyserActiveItemUse {
    private static final int LIVING_ENTITY_FLAGS = 8;
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

    public void reset() {
        this.hand = null;
    }

    public void onEntityData(ClientboundSetEntityDataPacket packet) {
        if (packet.getEntityId() != this.session.getPlayerEntity().getEntityId()) {
            return;
        }
        for (EntityMetadata<?, ?> value : packet.getMetadata()) {
            if (value.getId() == LIVING_ENTITY_FLAGS && value instanceof ByteEntityMetadata flags) {
                this.update(flags.getPrimitiveValue());
                return;
            }
        }
    }

    private void update(byte flags) {
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

    public boolean suppress(InventoryTransactionPacket packet) {
        if (this.hand == null || packet.getTransactionType() != InventoryTransactionType.ITEM_USE || packet.getActionType() != 0 || packet.getTriggerType() != TriggerType.SIMULATION_TICK) {
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
                && item != null
                && this.identifier.equals(item.getDefinition().getIdentifier())
                && (!item.isUsingNetId() || item.getNetId() == this.netId);
    }
}
