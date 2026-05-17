package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SetEntityDataListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SetEntityDataListener();

    private SetEntityDataListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        EntityPacketHandler handler = user.entityPacketHandlers().get(id);
        if (handler != null) {
            handler.handleSetEntityData(serverPlayer, event);
            return;
        }
        if (Config.interceptEntityName()) {
            boolean isChanged = false;
            List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
            for (int i = packedItems.size() - 1; i >= 0; i--) {
                Object packedItem = packedItems.get(i);
                int entityDataId = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(packedItem);
                if (entityDataId != BaseEntityData.CustomName.id()) continue;
                Optional<Object> optionalTextComponent = EntityUtils.getEntityDataValue(packedItem, BaseEntityData.CustomName);
                if (optionalTextComponent.isEmpty()) continue;
                Object textComponent = optionalTextComponent.get();
                String json = ComponentUtils.minecraftToJson(textComponent);
                Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(json);
                if (tokens.isEmpty()) continue;
                Component component = AdventureHelper.jsonToComponent(json);
                component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(serverPlayer));
                SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, Optional.of(ComponentUtils.adventureToMinecraft(component)));
                isChanged = true;
                break;
            }
            if (isChanged) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(id);
                PacketUtils.clientboundSetEntityDataPacket$pack(packedItems, buf);
            }
        }
    }
}
