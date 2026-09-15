package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.LivingEntityData;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.List;

/**
 * 玩家自身实体的包处理器：血量 metadata 缩放（DATA_HEALTH_ID 仍在 SynchedEntityData 里）
 */
public final class SelfPlayerPacketHandler extends EquipmentEntityPacketHandler {
    public static final SelfPlayerPacketHandler INSTANCE = new SelfPlayerPacketHandler();

    private SelfPlayerPacketHandler() {}

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        if (!Config.enableHealthScaling()) return;
        BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
        double maxHealth = serverPlayer.clientSideMaxHealth();
        if (maxHealth <= Config.healthScalingThreshold()) return;
        double visualMax = Config.healthScalingVisualMaxHealth();

        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
        for (int i = packedItems.size() - 1; i >= 0; i--) {
            Object packedItem = packedItems.get(i);
            if (SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(packedItem) != LivingEntityData.Health.id()) continue;
            float health = EntityUtils.getEntityDataValue(packedItem, LivingEntityData.Health);
            float scaled = (float) Math.clamp(health * visualMax / maxHealth, 0d, visualMax);
            SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, scaled);
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(id);
            PacketUtils.clientboundSetEntityDataPacket$pack(packedItems, buf);
            return;
        }
    }
}
