package net.momirealms.craftengine.bukkit.plugin.network.listener.configuration;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.Dialog;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.DialogTypes;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RegistryDataListener implements ByteBufferPacketListener {
    public static final RegistryDataListener INSTANCE = new RegistryDataListener();
    private static final Key ENCHANTMENT = Key.of("enchantment");
    private static final Key DIALOG = Key.of("dialog");

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!VersionHelper.isOrAbove1_21) return;
        FriendlyByteBuf buf = event.getBuffer();
        Key registryId = buf.readKey();
        Player player = (Player) user;
        if (registryId.equals(ENCHANTMENT)) {
            List<Entry> entries = buf.readList(Entry::read);
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeKey(registryId);
            buf.writeCollection(entries, (b, e) -> {
                e.data.ifPresent(this::createSafeEnchantment);
                e.write(b);
            });
        } else if (registryId.equals(DIALOG)) {
            if (!Config.interceptDialog()) return;
            List<Entry> entries = buf.readList(Entry::read);
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeKey(registryId);
            buf.writeCollection(entries, (b, e) -> {
                e.data.ifPresent(dialogTag -> {
                    Dialog dialog = DialogTypes.read((CompoundTag) dialogTag);
                    MutableBoolean changed = new MutableBoolean(false);
                    dialog.applyClientboundData(item -> {
                        Optional<Item> remapped = BukkitItemManager.instance().s2c(item, player);
                        if (remapped.isEmpty()) {
                            return item;
                        }
                        changed.set(true);
                        return remapped.get();
                    });
                    dialog.replaceNetworkTags(component -> {
                        Map<String, ComponentProvider> tokens = BukkitNetworkManager.instance().matchNetworkTags(AdventureHelper.componentToNbt(component));
                        if (tokens.isEmpty()) return component;
                        changed.set(true);
                        return AdventureHelper.replaceText(component, tokens, NetworkTextReplaceContext.of(player));
                    });
                    if (changed.booleanValue()) {
                        e.setData(Optional.of(dialog.save()));
                    }
                });
                e.write(b);
            });
        }
    }

    // 自定义效果里可能有自定义方块，防止客户端解码错误
    // 目前看来effects完全在服务器实现，所以移除effects是安全的
    private void createSafeEnchantment(Tag tag) {
        CompoundTag root = (CompoundTag) tag;
        root.remove("effects");
    }

    public static class Entry {
        private final Key id;
        private Optional<Tag> data;

        public Entry(Key id, Optional<Tag> data) {
            this.id = id;
            this.data = data;
        }

        public void setData(Optional<Tag> data) {
            this.data = data;
        }

        public static Entry read(FriendlyByteBuf buf) {
            Key id = buf.readKey();
            Optional<Tag> data = buf.readOptional(b -> b.readNbt(false));
            return new Entry(id, data);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeKey(this.id);
            buf.writeOptional(this.data, (b, t) -> b.writeNbt(t, false));
        }
    }
}
