package net.momirealms.craftengine.bedrock.geyser.block;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Set;

public final class GeyserBlockPlacementHandler {
    private static final long REPEAT_INTERVAL_NANOS = 110_000_000L;
    private final GeyserSession session;
    private final Set<String> blockItems;
    private boolean placed;
    private long lastPlacement;

    GeyserBlockPlacementHandler(GeyserSession session, Set<String> blockItems) {
        this.session = session;
        this.blockItems = blockItems;
    }

    public void onTransaction(GeyserPacketEvent<InventoryTransactionPacket> event) {
        InventoryTransactionPacket packet = event.packet();
        if (packet.getTransactionType() != InventoryTransactionType.ITEM_USE || packet.getActionType() != 0) {
            return;
        }
        ItemData item = packet.getItemInHand();
        if (item == null || !this.blockItems.contains(item.getDefinition().getIdentifier())) {
            return;
        }
        long now = System.nanoTime();
        // Geyser's same-position deduplication misses repeats targeting the block just placed.
        // Pace automatic repeats across positions; a new explicit click always starts immediately.
        if (packet.getTriggerType() == ItemUseTransaction.TriggerType.SIMULATION_TICK && this.placed && now - this.lastPlacement < REPEAT_INTERVAL_NANOS) {
            event.setCancelled(true);
            this.session.getPlayerInventoryHolder().updateSlot(this.session.getPlayerInventory().getOffsetForHotbar(packet.getHotbarSlot()));
            return;
        }
        this.placed = true;
        this.lastPlacement = now;
    }
}
