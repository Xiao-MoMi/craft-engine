package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.ItemFrameData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class ItemFramePacketHandler implements EntityPacketHandler {
    public static final ItemFramePacketHandler INSTANCE = new ItemFramePacketHandler();
    private static long lastWarningTime = 0;

    @Override
    public void handleSetEntityData(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        for (int i = 0; i < packedItems.size(); i++) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            if (entityDataId != ItemFrameData.Item.id()) continue;
            Object nmsItemStack = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
            if (!CoreReflections.clazz$ItemStack.isInstance(nmsItemStack)) {
                long time = System.currentTimeMillis();
                if (time - lastWarningTime > 5000) {
                    BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
                    CraftEngine.instance().logger().severe("An issue was detected while applying item-related entity data for '" + serverPlayer.name() +
                            "'. Please execute the command '/ce debug entity-id " + serverPlayer.world().name() + " " + id + "' and provide a screenshot for further investigation.");
                    lastWarningTime = time;
                }
                continue;
            }
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
