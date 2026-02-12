package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EntityDataUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class CommonItemPacketHandler implements EntityPacketHandler {
    public static final CommonItemPacketHandler INSTANCE = new CommonItemPacketHandler();
    private static long lastWarningTime = 0;

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean changed = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        for (int i = packedItems.size() - 1; i >= 0; i--) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            if (entityDataId != EntityDataUtils.UNSAFE_ITEM_DATA_ID) continue;
            Object nmsItemStack = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
            if (!CoreReflections.clazz$ItemStack.isInstance(nmsItemStack)) {
                long time = System.currentTimeMillis();
                if (time - lastWarningTime > 5000) {
                    BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
                    CraftEngine.instance().logger().severe("An issue was detected while applying item-related entity data for '" + serverPlayer.name() +
                            "'. Please execute the command '/ce debug entity-id " + serverPlayer.world().name() + " " + id + "' and provide a screenshot for further investigation. Class: " + nmsItemStack.getClass() + ". Object: " + GsonHelper.get().toJson(nmsItemStack));
                    lastWarningTime = time;
                }
                continue;
            }
            ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsItemStack);
            Optional<ItemStack> optional = BukkitItemManager.instance().s2c(itemStack, user);
            if (optional.isEmpty()) break;
            changed = true;
            itemStack = optional.get();
            Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
            packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack)));
            break;
        }
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(id);
            FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
        }
    }
}