package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class ItemDisplayPacketHandler implements EntityPacketHandler {
    public static final ItemDisplayPacketHandler INSTANCE = new ItemDisplayPacketHandler();

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean changed = false;
        List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
        for (int i = packedItems.size() - 1; i >= 0; i--) {
            Object packedItem = packedItems.get(i);
            int entityDataId = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(packedItem);
            if (entityDataId != ItemDisplayEntityData.DisplayedItem.id()) continue;
            Object nmsItemStack = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(packedItem);
            ItemStack itemStack = CraftItemStackProxy.INSTANCE.asCraftMirror(nmsItemStack);
            Optional<ItemStack> optional = BukkitItemManager.instance().s2c(itemStack, user);
            if (optional.isEmpty()) break;
            changed = true;
            itemStack = optional.get();
            SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, CraftItemStackProxy.INSTANCE.asNMSCopy(itemStack));
            break;
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(id);
            PacketUtils.clientboundSetEntityDataPacket$pack(packedItems, buf);
        }
    }
}
