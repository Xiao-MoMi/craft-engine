package net.momirealms.craftengine.bukkit.plugin.network.listener.configuration;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.block.BukkitCustomBlockStateWrapper;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.core.block.BlockKeys;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.Dialog;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.DialogTypes;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RegistryDataListener implements ByteBufferPacketListener {
    public static final RegistryDataListener INSTANCE = VersionHelper.isOrAbove1_21 ? new RegistryDataListener() : null;
    private static final Key ENCHANTMENT = Key.of("enchantment");
    private static final Key DIALOG = Key.of("dialog");
    private static final String BLOCK_ID = VersionHelper.isOrAbove26_3 ? "id" : "Name";
    private static final String BLOCK_PROPERTIES = VersionHelper.isOrAbove26_3 ? "properties" : "Properties";

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
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
        if (!(root.get("effects") instanceof CompoundTag effects)) return;
        replaceAll(effects);
    }

    private static void replaceAll(CompoundTag tag) {
        for (String key : tag.keySet()) {
            Tag value = tag.get(key);
            if (BLOCK_ID.equals(key) && value instanceof StringTag s) {
                Key id = Key.of(s.value());
                if (Key.CRAFTENGINE_NAMESPACE.equals(id.namespace) && BukkitBlockManager.instance().createVanillaBlockState(id.asString()) instanceof BukkitCustomBlockStateWrapper state) {
                    BlockStateWrapper visual = state.visualBlockState();
                    if (visual == null) {
                        tag.putString(BLOCK_ID, BlockKeys.STONE.asString());
                    } else {
                        String newId = visual.ownerId().asString();
                        tag.putString(BLOCK_ID, newId);
                        Collection<String> propertyNames = visual.getPropertyNames();
                        if (!propertyNames.isEmpty()) {
                            CompoundTag properties = new CompoundTag();
                            for (String property : propertyNames) {
                                Object propertyValue = visual.getProperty(property);
                                if (propertyValue == null) continue;
                                properties.putString(property, String.valueOf(propertyValue));
                            }
                            tag.put(BLOCK_PROPERTIES, properties);
                        }
                    }
                }
            } else if ("immune_blocks".equals(key) || "blocks".equals(key)) {
                if (value instanceof StringTag s) {
                    String string = s.value();
                    if (!(string.charAt(0) == '#') && BukkitBlockManager.instance().createVanillaBlockState(string) instanceof BukkitCustomBlockStateWrapper state) {
                        BlockStateWrapper visual = state.visualBlockState();
                        if (visual == null) {
                            visual = BukkitBlockManager.instance().createVanillaBlockState("minecraft:stone");
                        }
                        tag.putString(key, visual.ownerId().asString());
                        Debugger.COMMON.debug(() -> "tag2=" + tag);
                    }
                } else if (value instanceof ListTag l) {
                    boolean isReplaceable = false;
                    for (int i = 0; i < l.size(); i++) {
                        String string = l.getString(i);
                        if (BukkitBlockManager.instance().createVanillaBlockState(string) instanceof BukkitCustomBlockStateWrapper state) {
                            BlockStateWrapper visual = state.visualBlockState();
                            if (visual == null) {
                                visual = BukkitBlockManager.instance().createVanillaBlockState("minecraft:stone");
                            }
                            l.set(i, new StringTag(visual.ownerId().asString()));
                            isReplaceable = true;
                        } else break;
                    }
                    if (isReplaceable) Debugger.COMMON.debug(() -> "tag3=" + tag);
                }
            } else if (value instanceof CompoundTag compoundTag) {
                replaceAll(compoundTag);
            } else if (value instanceof ListTag listTag) {
                for (Tag listValue : listTag) {
                    if (listValue instanceof CompoundTag compoundTag) {
                        replaceAll(compoundTag);
                    }
                }
            }
        }
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
