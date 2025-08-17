package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.AbstractMinecartData;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.network.PacketConsumers;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AbstractMinecartPacketHandler implements EntityPacketHandler {
    public static final AbstractMinecartPacketHandler INSTANCE = new AbstractMinecartPacketHandler();
    private static final BlockStateHandler HANDLER = VersionHelper.isOrAbove1_21_3() ? BlockStateHandler_1_21_3.INSTANCE : BlockStateHandler_1_20.INSTANCE;

    @Override
    public void handleSetEntityData(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
        for (int i = 0; i < packedItems.size(); i++) {
            Object packedItem = packedItems.get(i);
            int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
            Object blockState = HANDLER.handle(user, packedItem, entityDataId);
            if (blockState != null) {
                packedItems.set(i, blockState);
                isChanged = true;
            } else if (Config.interceptEntityName() && entityDataId == BaseEntityData.CustomName.id()) {
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

    interface BlockStateHandler {
        Object handle(NetWorkUser user, Object packedItem, int entityDataId);
    }

    static class BlockStateHandler_1_21_3 implements BlockStateHandler {
        protected static final BlockStateHandler INSTANCE = new BlockStateHandler_1_21_3();

        @Override
        public Object handle(NetWorkUser user, Object packedItem, int entityDataId) {
            if (entityDataId != AbstractMinecartData.CustomDisplayBlock.id()) return null;
            @SuppressWarnings("unchecked")
            Optional<Object> blockState = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
            if (blockState.isEmpty()) return null;
            int stateId = BlockStateUtils.blockStateToId(blockState.get());
            int newStateId;
            if (!user.clientModEnabled()) {
                newStateId = PacketConsumers.remap(stateId);
            } else {
                newStateId = PacketConsumers.remapMOD(stateId);
            }
            if (newStateId == stateId) return null;
            Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
            return FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(
                    entityDataId, serializer, Optional.of(BlockStateUtils.idToBlockState(newStateId))
            );
        }
    }

    static class BlockStateHandler_1_20 implements BlockStateHandler {
        protected static final BlockStateHandler INSTANCE = new BlockStateHandler_1_20();

        @Override
        public Object handle(NetWorkUser user, Object packedItem, int entityDataId) {
            if (entityDataId != AbstractMinecartData.DisplayBlock.id()) return null;
            int stateId = (int) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
            int newStateId;
            if (!user.clientModEnabled()) {
                newStateId = PacketConsumers.remap(stateId);
            } else {
                newStateId = PacketConsumers.remapMOD(stateId);
            }
            if (newStateId == stateId) return null;
            Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
            return FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, newStateId);
        }
    }
}
