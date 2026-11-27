package net.momirealms.craftengine.bedrock.geyser.item;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackResponsePacket;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

final class InventoryRefresh {
    long player;
    BitSet open;
    InventoryHolder<?> holder;
    boolean cursor;
    boolean full;
    boolean prune;

    void add(ItemStackResponsePacket packet, @Nullable InventoryHolder<?> openHolder) {
        this.prune = true;
        if (this.full || packet.getEntries().isEmpty()) {
            this.full = true;
            return;
        }
        for (ItemStackResponse response : packet.getEntries()) {
            if (response.getResult() != ItemStackResponseStatus.OK || response.getContainers().isEmpty()) {
                this.full = true;
                return;
            }
            for (ItemStackResponseContainer container : response.getContainers()) {
                FullContainerName name = container.getContainerName();
                if (name == null || name.getDynamicId() != null) {
                    this.full = true;
                    return;
                }
                for (ItemStackResponseSlot slot : container.getItems()) {
                    if (!this.trackSlot(openHolder, name, slot)) {
                        this.full = true;
                        return;
                    }
                }
            }
        }
    }

    private boolean trackSlot(@Nullable InventoryHolder<?> holder, FullContainerName name, ItemStackResponseSlot entry) {
        ContainerSlotType type = name.getContainer();
        if (type == ContainerSlotType.CURSOR) {
            this.cursor = true;
            return true;
        }
        int slot = entry.getSlot();
        int playerSlot = switch (type) {
            case HOTBAR, INVENTORY, HOTBAR_AND_INVENTORY -> slot >= 0 && slot < 36 ? (slot < 9 ? slot + 36 : slot) : -1;
            case ARMOR -> slot >= 0 && slot < 4 ? slot + 5 : -1;
            case OFFHAND -> slot == 0 || slot == 1 ? 45 : -1;
            default -> -1;
        };
        if (playerSlot >= 0) {
            this.player |= 1L << playerSlot;
            return true;
        }
        if (holder == null || (this.holder != null && this.holder != holder)) {
            return false;
        }
        int javaSlot = containerSlot(holder, name, entry);
        if (javaSlot < 0) {
            return false;
        }
        this.holder = holder;
        if (this.open == null) {
            this.open = new BitSet();
        }
        this.open.set(javaSlot);
        return true;
    }

    private static <T extends Inventory> int containerSlot(InventoryHolder<T> holder, FullContainerName name, ItemStackResponseSlot entry) {
        ItemStackRequestSlotData input = new ItemStackRequestSlotData(name.getContainer(), entry.getSlot(), entry.getStackNetworkId(), name);
        try {
            int javaSlot = holder.translator().bedrockSlotToJava(input);
            if (javaSlot >= 0 && javaSlot < holder.inventory().getSize()) {
                BedrockContainerSlot output = holder.translator().javaSlotToBedrockContainer(javaSlot, holder.inventory());
                if (output.container() == name.getContainer() && output.slot() == entry.getSlot()) {
                    return javaSlot;
                }
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
            // Some translators reject slots outside their active layout.
        }
        return -1;
    }
}
