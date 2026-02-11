package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.AbstractMinecartData;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MinecartPacketHandler implements EntityPacketHandler {
    public static final MinecartPacketHandler INSTANCE = new MinecartPacketHandler();
    private static final BlockStateHandler BLOCK_STATE_HANDLER = VersionHelper.isOrAbove1_21_5() ? BlockStateHandler_1_21_5.INSTANCE : BlockStateHandler_1_20.INSTANCE;

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        boolean isChanged = false;
        List<Object> packedItems = PacketUtils.clientboundSetEntityDataPacket$unpack(buf);
        for (int i = packedItems.size() - 1; i >= 0; i--) {
            Object packedItem = packedItems.get(i);
            int entityDataId = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(packedItem);
            if (BLOCK_STATE_HANDLER.handle(user, packedItem, entityDataId)) {
                isChanged = true;
            } else if (Config.interceptEntityName() && entityDataId == BaseEntityData.CustomName.id()) {
                Optional<Object> optionalTextComponent = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(packedItem);
                if (optionalTextComponent.isEmpty()) continue;
                Object textComponent = optionalTextComponent.get();
                String json = ComponentUtils.minecraftToJson(textComponent);
                Map<String, ComponentProvider> tokens = CraftEngine.instance().networkManager().matchNetworkTags(json);
                if (tokens.isEmpty()) continue;
                Component component = AdventureHelper.replaceText(AdventureHelper.jsonToComponent(json), tokens, NetworkTextReplaceContext.of(user));
                SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, Optional.of(ComponentUtils.adventureToMinecraft(component)));
                isChanged = true;
            }
        }
        if (isChanged) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(id);
            PacketUtils.clientboundSetEntityDataPacket$pack(packedItems, buf);
        }
    }

    interface BlockStateHandler {
        boolean handle(NetWorkUser user, Object packedItem, int entityDataId);
    }

    static class BlockStateHandler_1_21_5 implements BlockStateHandler {
        protected static final BlockStateHandler INSTANCE = new BlockStateHandler_1_21_5();

        @Override
        public boolean handle(NetWorkUser user, Object packedItem, int entityDataId) {
            if (entityDataId != AbstractMinecartData.CustomDisplayBlock.id()) return false;
            Optional<Object> blockState = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(packedItem);
            if (blockState.isEmpty()) return false;
            int stateId = BlockStateUtils.blockStateToId(blockState.get());
            int newStateId = BukkitNetworkManager.instance().remapBlockState(stateId, user.clientModEnabled());
            if (newStateId == stateId) return false;
            SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, Optional.of(BlockStateUtils.idToBlockState(newStateId)));
            return true;
        }
    }

    static class BlockStateHandler_1_20 implements BlockStateHandler {
        protected static final BlockStateHandler INSTANCE = new BlockStateHandler_1_20();

        @Override
        public boolean handle(NetWorkUser user, Object packedItem, int entityDataId) {
            if (entityDataId != AbstractMinecartData.DisplayBlock.id()) return false;
            int stateId = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(packedItem);
            int newStateId = BukkitNetworkManager.instance().remapBlockState(stateId, user.clientModEnabled());
            if (newStateId == stateId) return false;
            SynchedEntityDataProxy.DataValueProxy.INSTANCE.setValue(packedItem, newStateId);
            return true;
        }
    }
}
