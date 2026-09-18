package net.momirealms.craftengine.bukkit.plugin.network.handler;

import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.EquipmentLodTracker;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipmentPacketHandler implements EntityPacketHandler {
    public static final EquipmentPacketHandler INSTANCE = new EquipmentPacketHandler();

    protected EquipmentPacketHandler() {
    }

    @Override
    public boolean handleSetEquipment(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        if (Config.disableItemOperations()) return true;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return true;
        boolean changed = false;
        List<Pair<Object, Item>> slots = new ArrayList<>(4);
        int slotMask;
        do {
            slotMask = buf.readByte();
            Object equipmentSlot = EquipmentSlotProxy.VALUES[slotMask & 127];
            Item itemStack = PacketUtils.readItem(buf);
            Optional<Item> converted = this.convertEquipment(serverPlayer, entityId, slotMask & 127, itemStack);
            if (converted.isPresent()) {
                changed = true;
                itemStack = converted.get();
            }
            slots.add(Pair.of(equipmentSlot, itemStack));
        } while ((slotMask & -128) != 0);
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(entityId);
            int i = slots.size();
            for (int j = 0; j < i; ++j) {
                Pair<Object, Item> pair = slots.get(j);
                Enum<?> equipmentSlot = (Enum<?>) pair.getFirst();
                boolean bl = j != i - 1;
                int k = equipmentSlot.ordinal();
                buf.writeByte(bl ? k | -128 : k);
                PacketUtils.writeItem(buf, pair.getSecond());
            }
        }
        return true;
    }

    protected Optional<Item> convertEquipment(Player player, int entityId, int slot, Item item) {
        EquipmentLodTracker tracker = ((BukkitServerPlayer) player).equipmentLod();
        return tracker == null ? BukkitItemManager.instance().s2c(item, player, ItemPacketSource.SET_EQUIPMENT) : tracker.equipment(entityId, slot, item);
    }
}
