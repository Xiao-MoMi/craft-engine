package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.TextDisplayEntityData;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.List;
import java.util.Map;

public class TextDisplayPacketHandler implements EntityPacketHandler {
    public static final TextDisplayPacketHandler INSTANCE = new TextDisplayPacketHandler();

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        if (!Config.interceptTextDisplay()) {
            return;
        }
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
        for (int i = packedItems.size() - 1; i >= 0; i--) {
            Object packedItem = packedItems.get(i);
            int entityDataId = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(packedItem);
            if (entityDataId != TextDisplayEntityData.Text.id()) continue;
            Object textComponent = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(packedItem);
            if (textComponent == ComponentProxy.INSTANCE.empty()) break;
            String json = ComponentUtils.minecraftToJson(textComponent);
            Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(json);
            if (tokens.isEmpty()) break;
            Component component = AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of(user));
            SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, ComponentUtils.adventureToMinecraft(component));
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
