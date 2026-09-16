package net.momirealms.craftengine.bukkit.plugin.network;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;

import java.util.Map;
import java.util.Optional;

public final class EntityDataValueReplacer {
    private EntityDataValueReplacer() {}

    // 按序列化器分发，无需维护实体类型与数据 id 的对应关系
    public static Object replace(Player user, Object dataValue) {
        Object serializer = SynchedEntityDataProxy.DataValueProxy.INSTANCE.getSerializer(dataValue);
        if (serializer == EntityDataSerializersProxy.ITEM_STACK) {
            return Config.disableItemOperations() ? null : replaceItem(user, dataValue);
        } else if (serializer == EntityDataSerializersProxy.OPTIONAL_COMPONENT || serializer == EntityDataSerializersProxy.COMPONENT) {
            return Config.interceptEntityData() ? replaceComponent(user, dataValue, serializer) : null;
        } else if (serializer == EntityDataSerializersProxy.BLOCK_STATE || serializer == EntityDataSerializersProxy.OPTIONAL_BLOCK_STATE) {
            return replaceBlockState(user, dataValue, serializer);
        }
        return null;
    }

    private static Object replaceItem(Player user, Object dataValue) {
        Item item = ItemStackUtils.wrap(SynchedEntityDataProxy.DataValueProxy.INSTANCE.getValue(dataValue));
        // 一定要先复制，一个包可能发给多个玩家
        Optional<Item> converted = BukkitItemManager.instance().s2c(item.copy(), user, ItemPacketSource.ENTITY_DATA);
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
