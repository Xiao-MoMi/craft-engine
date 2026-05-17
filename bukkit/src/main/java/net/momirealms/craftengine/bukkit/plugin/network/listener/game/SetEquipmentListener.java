package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;

import java.util.List;
import java.util.Optional;

public final class SetEquipmentListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SetEquipmentListener();

    private SetEquipmentListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        boolean changed = false;
        int entity = buf.readVarInt();
        List<Pair<Object, Item>> slots = Lists.newArrayList();
        int slotMask;
        do {
            slotMask = buf.readByte();
            Object equipmentSlot = EquipmentSlotProxy.VALUES[slotMask & 127];
            Item itemStack = PacketUtils.readItem(buf);
            Optional<Item> optional = BukkitItemManager.instance().s2c(itemStack, serverPlayer);
            if (optional.isPresent()) {
                changed = true;
                itemStack = optional.get();
            }
            slots.add(Pair.of(equipmentSlot, itemStack));
        } while ((slotMask & -128) != 0);
        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(entity);
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
    }
}
