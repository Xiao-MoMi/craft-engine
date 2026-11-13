package net.momirealms.craftengine.bedrock.geyser.block;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;

import java.util.Set;
import java.util.function.LongSupplier;

public final class GeyserBlockPlacementHandler extends PacketTranslator<InventoryTransactionPacket> {
    private static final long REPEAT_INTERVAL_NANOS = 200_000_000L;
    private final PacketTranslator<InventoryTransactionPacket> delegate;
    private final Set<String> blockItems;
    private final LongSupplier clock;

    GeyserBlockPlacementHandler(PacketTranslator<InventoryTransactionPacket> delegate, Set<String> blockItems, LongSupplier clock) {
        this.delegate = delegate;
        this.blockItems = Set.copyOf(blockItems);
        this.clock = clock;
    }

    @SuppressWarnings("unchecked")
    public static GeyserBlockPlacementHandler install(Set<String> blockItems) {
        PacketTranslator<InventoryTransactionPacket> original = (PacketTranslator<InventoryTransactionPacket>) Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class);
        GeyserBlockPlacementHandler handler = new GeyserBlockPlacementHandler(original, blockItems, System::nanoTime);
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
        ItemData item = packet.getItemInHand();
        if (packet.getTransactionType() != InventoryTransactionType.ITEM_USE || packet.getActionType() != 0
                || item == null || !this.blockItems.contains(item.getDefinition().getIdentifier())) {
            this.delegate.translate(session, packet);
            return;
        }
        GeyserPlayer player = CraftEngineGeyserExtension.instance().playerManager().get(session);
        if (player == null) {
            this.delegate.translate(session, packet);
            return;
        }
        long now = this.clock.getAsLong();
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
