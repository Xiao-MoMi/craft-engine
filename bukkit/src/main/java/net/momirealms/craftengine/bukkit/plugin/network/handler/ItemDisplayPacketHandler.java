package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EntityDataUtils;
import net.momirealms.craftengine.core.plugin.network.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class ItemDisplayPacketHandler implements EntityPacketHandler {
    public static final ItemDisplayPacketHandler INSTANCE = new ItemDisplayPacketHandler();

    @Override
    public void handleSetEntityData(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        for (int i = 0; i < packedItems.size(); i++) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            if (entityDataId != EntityDataUtils.DISPLAYED_ITEM_DATA_ID) continue;
            Object nmsItemStack = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
            ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsItemStack);
            Optional<ItemStack> optional = BukkitItemManager.instance().s2c(itemStack, (BukkitServerPlayer) user);
            if (optional.isEmpty()) continue;
            isChanged = true;
            itemStack = optional.get();
            Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
            packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(
                    entityDataId, serializer, FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack)
            ));
            break;
        }
        if (isChanged) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(id);
            FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
        }
    }
}
