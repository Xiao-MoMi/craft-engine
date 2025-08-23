package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class ArmorStandPacketHandler implements EntityPacketHandler {
    public static final ArmorStandPacketHandler INSTANCE = new ArmorStandPacketHandler();

    @Override
    public void handleSetEntityData(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.interceptArmorStand()) {
            return;
        }
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        for (int i = 0; i < packedItems.size(); i++) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            if (entityDataId != BaseEntityData.CustomName.id()) continue;
            @SuppressWarnings("unchecked")
            Optional<Object> optionalTextComponent = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
            if (optionalTextComponent.isEmpty()) continue;
            Object textComponent = optionalTextComponent.get();
            String json = ComponentUtils.minecraftToJson(textComponent);
            Map<String, Component> tokens = CraftEngine.instance().fontManager().matchTags(json);
            if (tokens.isEmpty()) continue;
            Component component = AdventureHelper.jsonToComponent(json);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
            packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, Optional.of(ComponentUtils.adventureToMinecraft(component))));
            isChanged = true;
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
