package net.momirealms.craftengine.bedrock.geyser.block;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;

import java.util.Set;

public final class GeyserBlockPlacementHandler extends PacketTranslator<InventoryTransactionPacket> {
    private static final long REPEAT_INTERVAL_NANOS = 110_000_000L;
    private final PacketTranslator<InventoryTransactionPacket> delegate;
    private final GeyserPlayerManager players;
    private final Set<String> blockItems;

    private GeyserBlockPlacementHandler(PacketTranslator<InventoryTransactionPacket> delegate, GeyserPlayerManager players, Set<String> blockItems) {
        this.delegate = delegate;
        this.players = players;
        this.blockItems = Set.copyOf(blockItems);
    }

    @SuppressWarnings("unchecked")
    public static GeyserBlockPlacementHandler install(GeyserPlayerManager players, Set<String> blockItems) {
        PacketTranslator<InventoryTransactionPacket> original = (PacketTranslator<InventoryTransactionPacket>) Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class);
        GeyserBlockPlacementHandler handler = new GeyserBlockPlacementHandler(original, players, blockItems);
        Registries.BEDROCK_PACKET_TRANSLATORS.register(InventoryTransactionPacket.class, handler);
        return handler;
    }

    public void uninstall() {
        if (Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class) == this) {
            Registries.BEDROCK_PACKET_TRANSLATORS.register(InventoryTransactionPacket.class, this.delegate);
        }
    }

    @Override
    public void translate(GeyserSession session, InventoryTransactionPacket packet) {
        if (packet.getTransactionType() != InventoryTransactionType.ITEM_USE || packet.getActionType() != 0) {
            this.delegate.translate(session, packet);
            return;
        }
        ItemData item = packet.getItemInHand();
        if (item == null || !this.blockItems.contains(item.getDefinition().getIdentifier())) {
            this.delegate.translate(session, packet);
            return;
        }
        GeyserPlayer player = this.players.get(session);
        if (player == null) {
            this.delegate.translate(session, packet);
            return;
        }
        long now = System.nanoTime();
        // Geyser's same-position deduplication misses repeats targeting the block just placed.
        // Pace automatic repeats across positions; a new explicit click always starts immediately.
        if (packet.getTriggerType() == ItemUseTransaction.TriggerType.SIMULATION_TICK && player.placementWithin(now, REPEAT_INTERVAL_NANOS)) {
            session.getPlayerInventoryHolder().updateSlot(session.getPlayerInventory().getOffsetForHotbar(packet.getHotbarSlot()));
            return;
        }
        player.placed(now);
        this.delegate.translate(session, packet);
    }
}
