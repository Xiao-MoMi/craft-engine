package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.IntTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.StringTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.TagParserProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTDataComponentValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SystemChatListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new V1_20_3() : new V1_20();

    private SystemChatListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptSystemChat() && Config.disableItemOperations()) return;
            FriendlyByteBuf buf = event.getBuffer();
            String jsonOrPlainString = buf.readUtf();
            Tag tag = RegistryOps.JSON.convertTo(RegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(jsonOrPlainString, JsonElement.class));
            Component component = AdventureHelper.nbtToComponent(tag);
            boolean overlay = buf.readBoolean();
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            if (Config.interceptSystemChat()) {
                Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(jsonOrPlainString);
                if (!tokens.isEmpty()) {
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                }
            }
            if (!Config.disableItemOperations()) {
                component = AdventureHelper.replaceShowItem(component, s -> replaceShowItem(s, (BukkitServerPlayer) user));
            }
            buf.writeUtf(RegistryOps.SPARROW_NBT.convertTo(RegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString());
            buf.writeBoolean(overlay);
        }
    }

    private static class V1_20_3 implements ByteBufferPacketListener {
        private V1_20_3() {}

        @Override
        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (!Config.interceptSystemChat() && Config.disableItemOperations()) return;
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            boolean overlay = buf.readBoolean();
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            Component component = AdventureHelper.tagToComponent(nbt);
            if (Config.interceptSystemChat()) {
                Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(nbt);
                if (!tokens.isEmpty()) {
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                }
            }
            if (!Config.disableItemOperations()) {
                component = AdventureHelper.replaceShowItem(component, s -> replaceShowItem(s, (BukkitServerPlayer) user));
            }
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
            buf.writeBoolean(overlay);
        }
    }

    @SuppressWarnings({"deprecation", "PatternValidation"})
    private static HoverEvent.ShowItem replaceShowItem(HoverEvent.ShowItem showItem, BukkitServerPlayer player) {
        Object nmsItemStack;
        if (VersionHelper.COMPONENT_RELEASE) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("count", showItem.count());
            itemTag.putString("id", showItem.item().asMinimalString());
            Map<net.kyori.adventure.key.Key, DataComponentValue> components = showItem.dataComponents();
            if (!components.isEmpty()) {
                CompoundTag componentsTag = new CompoundTag();
                Map<net.kyori.adventure.key.Key, NBTDataComponentValue> componentsMap = showItem.dataComponentsAs(NBTDataComponentValue.class);
                for (Map.Entry<net.kyori.adventure.key.Key, NBTDataComponentValue> entry : componentsMap.entrySet()) {
                    componentsTag.put(entry.getKey().asMinimalString(), entry.getValue().tag());
                }
                itemTag.put("components", componentsTag);
            }
            DataResult<Object> nmsItemStackResult = ItemStackProxy.INSTANCE.getCodec().parse(RegistryOps.SPARROW_NBT, itemTag);
            Optional<Object> result = nmsItemStackResult.result();
            if (result.isEmpty()) {
                return showItem;
            }
            nmsItemStack = result.get();
        } else {
            Object compoundTag = CompoundTagProxy.INSTANCE.newInstance();
            CompoundTagProxy.INSTANCE.put(compoundTag, "Count", IntTagProxy.INSTANCE.valueOf(showItem.count()));
            CompoundTagProxy.INSTANCE.put(compoundTag, "id", StringTagProxy.INSTANCE.valueOf(showItem.item().asMinimalString()));
            BinaryTagHolder nbt = showItem.nbt();
            if (nbt != null) {
                try {
                    Object nmsTag = TagParserProxy.INSTANCE.parseCompoundFully(nbt.string());
                    CompoundTagProxy.INSTANCE.put(compoundTag, "tag", nmsTag);
                } catch (CommandSyntaxException ignored) {
                    return showItem;
                }
            }
            nmsItemStack = ItemStackProxy.INSTANCE.of(compoundTag);
        }

        BukkitItemManager itemManager = BukkitItemManager.instance();
        Item wrap = itemManager.wrap(ItemStackUtils.getBukkitStack(nmsItemStack));
        Optional<Item> remapped = itemManager.s2c(wrap, player);
        if (remapped.isEmpty()) {
            return showItem;
        }

        Item clientBoundItem = remapped.get();
        net.kyori.adventure.key.Key id = KeyUtils.toAdventureKey(clientBoundItem.vanillaId());
        int count = clientBoundItem.count();
        if (VersionHelper.COMPONENT_RELEASE) {
            DataResult<Tag> tagDataResult = ItemStackProxy.INSTANCE.getCodec().encodeStart(RegistryOps.SPARROW_NBT, clientBoundItem.minecraftItem());
            Optional<Tag> result = tagDataResult.result();
            if (result.isEmpty()) {
                return showItem;
            }
            CompoundTag itemTag = (CompoundTag) result.get();
            CompoundTag componentsTag = itemTag.getCompound("components");
            if (componentsTag != null) {
                Map<net.kyori.adventure.key.Key, NBTDataComponentValue> componentsMap = new HashMap<>();
                for (Map.Entry<String, Tag> entry : componentsTag.entrySet()) {
                    componentsMap.put(net.kyori.adventure.key.Key.key(entry.getKey()), NBTDataComponentValue.of(entry.getValue()));
                }
                return HoverEvent.ShowItem.showItem(id, count, componentsMap);
            } else {
                return HoverEvent.ShowItem.showItem(id, count);
            }
        } else {
            Object tag = ItemStackProxy.INSTANCE.getTag(clientBoundItem.minecraftItem());
            if (tag != null) {
                return HoverEvent.ShowItem.showItem(id, count, BinaryTagHolder.binaryTagHolder(tag.toString()));
            } else {
                return HoverEvent.ShowItem.showItem(id, count);
            }
        }
    }
}
