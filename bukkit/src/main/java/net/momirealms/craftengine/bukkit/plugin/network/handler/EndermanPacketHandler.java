package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.EnderManData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EndermanPacketHandler implements EntityPacketHandler {
    public static final EndermanPacketHandler INSTANCE = new EndermanPacketHandler();

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        for (int i = 0; i < packedItems.size(); i++) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            if (entityDataId == EnderManData.CarryState.id()) {
                @SuppressWarnings("unchecked")
                Optional<Object> blockState = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                if (blockState.isEmpty()) continue;
                int stateId = BlockStateUtils.blockStateToId(blockState.get());
                int newStateId = BukkitNetworkManager.instance().remapBlockState(stateId, user.clientModEnabled());
                if (newStateId == stateId) continue;
                Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(
                        entityDataId, serializer, Optional.of(BlockStateUtils.idToBlockState(newStateId))
                ));
                isChanged = true;
            } else if (Config.interceptEntityName() && entityDataId == BaseEntityData.CustomName.id()) {
                @SuppressWarnings("unchecked")
                Optional<Object> optionalTextComponent = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                if (optionalTextComponent.isEmpty()) continue;
                Object textComponent = optionalTextComponent.get();
                String json = ComponentUtils.minecraftToJson(textComponent);
                Map<String, ComponentProvider> tokens = CraftEngine.instance().fontManager().matchTags(json);
                if (tokens.isEmpty()) continue;
                Component component = AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of(user));
                Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(
                        entityDataId, serializer, Optional.of(ComponentUtils.adventureToMinecraft(component))
                ));
                isChanged = true;
            }
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
