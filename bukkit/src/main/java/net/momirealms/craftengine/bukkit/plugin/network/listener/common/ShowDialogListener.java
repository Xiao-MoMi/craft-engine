package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import com.mojang.datafixers.util.Either;
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
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;
import java.util.Optional;

public final class ShowDialogListener implements ByteBufferPacketListener {
    public static final ShowDialogListener INSTANCE = new ShowDialogListener();

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.interceptDialog()) return;
        FriendlyByteBuf buf = event.getBuffer();
        Either<Integer, Tag> directOrRef = buf.readHolder(it -> it.readNbt(false));
        Player player = (Player) user;
        directOrRef.ifRight(tag -> {
            Dialog dialog = DialogTypes.read((CompoundTag) tag);
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
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(0);
                buf.writeNbt(dialog.save(), false);
            }
        });
    }
}
