package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.font.EmojiTextProcessResult;
import net.momirealms.craftengine.core.font.EmojiUseCase;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ChatTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamCodecProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSystemChatPacketProxy;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.UUID;

public final class PlayerChatListener {
    public static final ByteBufferPacketListener INSTANCE = VersionHelper.isOrAbove1_20_3 ? new V1_20_3() : new V1_20();

    private PlayerChatListener() {}

    private static class V1_20 implements ByteBufferPacketListener {
        private V1_20() {}

        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableChatReport()) {
                convertToSystemChat(user, event);
                return;
            }
            if (!Config.interceptPlayerChat()) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            UUID sender = buf.readUUID();
            int index = buf.readVarInt();
            byte @Nullable [] messageSignature = buf.readNullable(b -> {
                byte[] bs = new byte[256];
                buf.readBytes(bs);
                return bs;
            });
            // SignedMessageBody.Packed start
            String content = buf.readUtf(256);
            Instant timeStamp = buf.readInstant();
            long salt = buf.readLong();
            // LastSeenMessages.Packed start
            ArrayList<Pair<Integer, byte[]>> lastSeen = buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), b -> {
                int i = b.readVarInt() - 1;
                if (i == -1) {
                    byte[] bs = new byte[256];
                    buf.readBytes(bs);
                    return Pair.of(-1, bs);
                } else {
                    return Pair.of(i, null);
                }
            });
            // LastSeenMessages.Packed end
            // SignedMessageBody.Packed end
            @Nullable String unsignedContent = buf.readNullable(FriendlyByteBuf::readUtf);
            BukkitNetworkManager networkManager = BukkitNetworkManager.instance();
            if (unsignedContent != null) {
                Map<String, ComponentProvider> unsignedContentTokens = networkManager.matchNetworkTags(unsignedContent);
                if (!unsignedContentTokens.isEmpty()) {
                    Tag tag = RegistryOps.JSON.convertTo(RegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(unsignedContent, JsonElement.class));
                    Component component = AdventureHelper.nbtToComponent(tag);
                    component = AdventureHelper.replaceText(component, unsignedContentTokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    unsignedContent = RegistryOps.SPARROW_NBT.convertTo(RegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString();
                    changed = true;
                }
            }
            // FilterMask start
            int type = buf.readVarInt();
            BitSet mask = type == 2 /* PARTIALLY_FILTERED */ ? buf.readBitSet() : null;
            // FilterMask end
            // ChatType.BoundNetwork start
            int chatType = buf.readVarInt();
            String name = buf.readUtf();
            Map<String, ComponentProvider> nameTokens = networkManager.matchNetworkTags(name);
            if (!nameTokens.isEmpty()) {
                Tag tag = RegistryOps.JSON.convertTo(RegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(name, JsonElement.class));
                Component component = AdventureHelper.nbtToComponent(tag);
                component = AdventureHelper.replaceText(component, nameTokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                name = RegistryOps.SPARROW_NBT.convertTo(RegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString();
                changed = true;
            }
            @Nullable String targetName = buf.readNullable(FriendlyByteBuf::readUtf);
            if (targetName != null) {
                Map<String, ComponentProvider> targetNameTokens = networkManager.matchNetworkTags(targetName);
                if (!targetNameTokens.isEmpty()) {
                    Tag tag = RegistryOps.JSON.convertTo(RegistryOps.SPARROW_NBT, GsonHelper.get().fromJson(targetName, JsonElement.class));
                    Component component = AdventureHelper.nbtToComponent(tag);
                    component = AdventureHelper.replaceText(component, targetNameTokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    targetName = RegistryOps.SPARROW_NBT.convertTo(RegistryOps.JSON, AdventureHelper.componentToNbt(component)).toString();
                    changed = true;
                }
            }
            // ChatType.BoundNetwork end
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(sender);
                buf.writeVarInt(index);
                buf.writeNullable(messageSignature, (b, bs) -> buf.writeBytes(bs));
                buf.writeUtf(content);
                buf.writeInstant(timeStamp);
                buf.writeLong(salt);
                buf.writeCollection(lastSeen, (b, pair) -> {
                    b.writeVarInt(pair.left() + 1);
                    if (pair.right() != null) {
                        b.writeBytes(pair.right());
                    }
                });
                buf.writeNullable(unsignedContent, FriendlyByteBuf::writeUtf);
                buf.writeVarInt(type);
                if (type == 2) buf.writeBitSet(mask);
                buf.writeVarInt(chatType);
                buf.writeUtf(name);
                buf.writeNullable(targetName, FriendlyByteBuf::writeUtf);
            }
        }
    }

    private static class V1_20_3 implements ByteBufferPacketListener {
        private V1_20_3() {}

        public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
            if (Config.disableChatReport()) {
                convertToSystemChat(user, event);
                return;
            }
            if (!Config.interceptPlayerChat()) return;
            FriendlyByteBuf buf = event.getBuffer();
            boolean changed = false;
            int globalIndex = VersionHelper.isOrAbove1_21_5 ? buf.readVarInt() : -1;
            UUID sender = buf.readUUID();
            int index = buf.readVarInt();
            byte @Nullable [] messageSignature = buf.readNullable(b -> {
                byte[] bs = new byte[256];
                buf.readBytes(bs);
                return bs;
            });
            // SignedMessageBody.Packed start
            String content = buf.readUtf(256);
            Instant timeStamp = buf.readInstant();
            long salt = buf.readLong();
            // LastSeenMessages.Packed start
            ArrayList<Pair<Integer, byte[]>> lastSeen = buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), b -> {
                int i = b.readVarInt() - 1;
                if (i == -1) {
                    byte[] bs = new byte[256];
                    buf.readBytes(bs);
                    return Pair.of(-1, bs);
                } else {
                    return Pair.of(i, null);
                }
            });
            // LastSeenMessages.Packed end
            // SignedMessageBody.Packed end
            @Nullable Tag unsignedContent = buf.readNullable(b -> b.readNbt(false));
            BukkitNetworkManager networkManager = BukkitNetworkManager.instance();
            if (unsignedContent != null) {
                Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(unsignedContent);
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(unsignedContent);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    unsignedContent = AdventureHelper.componentToTag(component);
                    changed = true;
                }
            }
            // FilterMask start
            int type = buf.readVarInt();
            BitSet mask = type == 2 /* PARTIALLY_FILTERED */ ? buf.readBitSet() : null;
            // FilterMask end
            // ChatType.Bound start
            int chatType = buf.readVarInt();
            Tag name = buf.readNbt(false);
            if (name != null) {
                Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(name);
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(name);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    name = AdventureHelper.componentToTag(component);
                    changed = true;
                }
            }
            @Nullable Tag targetName = buf.readNullable(b -> b.readNbt(false));
            if (targetName != null) {
                Map<String, ComponentProvider> tokens = networkManager.matchNetworkTags(targetName);
                if (!tokens.isEmpty()) {
                    Component component = AdventureHelper.tagToComponent(targetName);
                    component = AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of((BukkitServerPlayer) user));
                    targetName = AdventureHelper.componentToTag(component);
                    changed = true;
                }
            }
            // ChatType.Bound end
            if (changed) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                if (VersionHelper.isOrAbove1_21_5) buf.writeVarInt(globalIndex);
                buf.writeUUID(sender);
                buf.writeVarInt(index);
                buf.writeNullable(messageSignature, (b, bs) -> buf.writeBytes(bs));
                buf.writeUtf(content);
                buf.writeInstant(timeStamp);
                buf.writeLong(salt);
                buf.writeCollection(lastSeen, (b, pair) -> {
                    b.writeVarInt(pair.left() + 1);
                    if (pair.right() != null) {
                        b.writeBytes(pair.right());
                    }
                });
                buf.writeNullable(unsignedContent, (b, tag) -> b.writeNbt(tag, false));
                buf.writeVarInt(type);
                if (type == 2) buf.writeBitSet(mask);
                buf.writeVarInt(chatType);
                buf.writeNbt(name, false);
                buf.writeNullable(targetName, (b, tag) -> b.writeNbt(tag, false));
            }
        }
    }

    private static void convertToSystemChat(NetWorkUser user, ByteBufPacketEvent event) {
        event.setCancelled(true);
        FriendlyByteBuf buf = event.getBuffer();
        /*globalIndex*/ if (VersionHelper.isOrAbove1_21_5) buf.readVarInt();
        UUID sender = buf.readUUID();
        /*index*/ buf.readVarInt();
        /*signature*/ buf.readNullable(it -> it.readFixedBytes(256));
        String bodyContent = buf.readUtf(256);
        /*bodyTimeStamp*/ buf.readInstant();
        /*bodySalt*/ buf.readLong();
        /*bodyLastSeen*/ buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), it -> {
            if (it.readVarInt() == 0) {
                it.readFixedBytes(256);
            }
            return null; // 我们只读取不在乎返回值
        });
        Component unsignedContent = buf.readNullable(FriendlyByteBuf::readComponent);
        int filterMaskType = buf.readVarInt();
        /*filterMaskMask*/ if (filterMaskType == 2 /*PARTIALLY_FILTERED*/) buf.readBitSet();
        Object chatType; // 这里需要和注册表交互，太复杂了直接用 nms 解决
        ByteBuf nmsFriendlyByteBuf = PacketUtils.ensureNMSFriendlyByteBuf(buf);
        if (VersionHelper.isOrAbove1_20_5) {
            chatType = StreamCodecProxy.INSTANCE.decode(ChatTypeProxy.BoundProxy.STREAM_CODEC, nmsFriendlyByteBuf);
        } else {
            chatType = ChatTypeProxy.BoundNetworkProxy.INSTANCE.resolve(
                    ChatTypeProxy.BoundNetworkProxy.INSTANCE.newInstance(nmsFriendlyByteBuf),
                    RegistryUtils.getRegistryAccess()
            ).orElseThrow();
        }
        // 过滤非法字符在 BukkitFontManager#processChatEvent 完成
        Object content = unsignedContent != null ? ComponentUtils.adventureToMinecraft(unsignedContent) : ComponentProxy.INSTANCE.literal(bodyContent);
        Object decorate = ChatTypeProxy.BoundProxy.INSTANCE.decorate(chatType, content);
        if (unsignedContent == null && Config.allowEmojiChat()) { // 如果不为 null 表明已经在 BukkitFontManager#processChatEvent 处理完成
            String rawJsonMessage = ComponentUtils.minecraftToJson(decorate);
            @javax.annotation.Nullable Player chatSender = CraftEngine.instance().platform().getPlayer(sender);
            EmojiTextProcessResult result = BukkitFontManager.instance().replaceJsonEmoji(rawJsonMessage, chatSender, EmojiUseCase.CHAT);
            if (result.replaced()) {
                decorate = ComponentUtils.jsonToMinecraft(result.text());
            }
        }
        Object systemChatPacket = ClientboundSystemChatPacketProxy.INSTANCE.newInstance(decorate, false);
        user.sendPacket(systemChatPacket, false);
    }
}
