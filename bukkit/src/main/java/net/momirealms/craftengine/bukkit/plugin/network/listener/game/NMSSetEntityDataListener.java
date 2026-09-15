package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NMSSetEntityDataListener implements NMSPacketListener {
    public static final NMSSetEntityDataListener INSTANCE = new NMSSetEntityDataListener();

    private NMSSetEntityDataListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (!(user instanceof BukkitServerPlayer serverPlayer)) return;
        List<Object> packedItems = ClientboundSetEntityDataPacketProxy.INSTANCE.getPackedItems(packet);
        List<Object> newItems = null;
        for (int i = 0, size = packedItems.size(); i < size; i++) {
            Object dataValue = packedItems.get(i);
            Object serializer = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getSerializer(dataValue);
            Object newDataValue;
            if (serializer == EntityDataSerializersProxy.ITEM_STACK) {
                newDataValue = Config.disableItemOperations() ? null : replaceItem(serverPlayer, dataValue);
            } else if (serializer == EntityDataSerializersProxy.OPTIONAL_COMPONENT || serializer == EntityDataSerializersProxy.COMPONENT) {
                newDataValue = Config.interceptEntityData() ? replaceComponent(serverPlayer, dataValue, serializer) : null;
            } else if (serializer == EntityDataSerializersProxy.BLOCK_STATE || serializer == EntityDataSerializersProxy.OPTIONAL_BLOCK_STATE) {
                newDataValue = replaceBlockState(serverPlayer, dataValue, serializer);
            } else {
                continue;
            }
            if (newDataValue == null) continue;
            if (newItems == null) newItems = new ArrayList<>(packedItems);
            newItems.set(i, newDataValue);
        }
        if (newItems != null) {
            PacketUtils.replacePacket(event, packet, ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(ClientboundSetEntityDataPacketProxy.INSTANCE.getId(packet), newItems));
        }
    }

    private static Object replaceItem(Player user, Object dataValue) {
        Item item = ItemStackUtils.wrap(SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(dataValue));
        // 一定要先复制，一个包可能发给多个玩家
        Optional<Item> converted = BukkitItemManager.instance().s2c(item.copy(), user);
        if (converted.isPresent()) {
            return SynchedEntityDataProxy.DataValueProxy.INSTANCE.newInstance(
                    SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(dataValue),
                    SynchedEntityDataProxy.DataValueProxy.INSTANCE.getSerializer(dataValue),
                    converted.get().minecraftItem()
            );
        }
        return null;
    }

    private static Object replaceComponent(Player user, Object dataValue, Object serializer) {
        boolean optionalComponent = serializer == EntityDataSerializersProxy.OPTIONAL_COMPONENT;
        Object rawValue = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(dataValue);
        Object textComponent;
        if (optionalComponent) {
            if (!(rawValue instanceof Optional<?> optional) || optional.isEmpty()) return null;
            textComponent = optional.get();
        } else {
            if (rawValue == ComponentProxy.INSTANCE.empty()) return null;
            textComponent = rawValue;
        }
        if (!ComponentUtils.hasNetworkTag(textComponent)) return null;
        JsonElement json = ComponentUtils.minecraftToJsonElement(textComponent);
        Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(json);
        if (tokens.isEmpty()) return null;
        Component component = AdventureHelper.replaceText(AdventureHelper.jsonElementToComponent(json), tokens, NetworkTextReplaceContext.of(user));
        Object minecraftComponent = ComponentUtils.adventureToMinecraft(component);
        return SynchedEntityDataProxy.DataValueProxy.INSTANCE.newInstance(
                SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(dataValue),
                serializer,
                optionalComponent ? Optional.of(minecraftComponent) : minecraftComponent
        );
    }

    private static Object replaceBlockState(Player user, Object dataValue, Object serializer) {
        boolean optionalBlockState = serializer == EntityDataSerializersProxy.OPTIONAL_BLOCK_STATE;
        Object rawValue = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(dataValue);
        Object blockState;
        if (optionalBlockState) {
            if (!(rawValue instanceof Optional<?> optional) || optional.isEmpty()) return null;
            blockState = optional.get();
        } else {
            blockState = rawValue;
        }
        int stateId = BlockStateUtils.blockStateToId(blockState);
        int newStateId = BukkitNetworkManager.instance().remapBlockState(stateId, user.clientCustomBlockEnabled());
        if (newStateId == stateId) return null;
        Object newBlockState = BlockStateUtils.idToBlockState(newStateId);
        return SynchedEntityDataProxy.DataValueProxy.INSTANCE.newInstance(
                SynchedEntityDataProxy.DataValueProxy.INSTANCE.getId(dataValue),
                serializer,
                optionalBlockState ? Optional.of(newBlockState) : newBlockState
        );
    }
}
