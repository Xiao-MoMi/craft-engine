package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.RecipeDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.Optional;

public final class PlaceGhostRecipeListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new PlaceGhostRecipeListener();

    private PlaceGhostRecipeListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (Config.disableItemOperations()) return;
        if (!VersionHelper.isOrAbove1_21_2) return;
        MutableBoolean changed = new MutableBoolean(false);
        FriendlyByteBuf buf = event.getBuffer();
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        BukkitItemManager itemManager = BukkitItemManager.instance();
        int containerId = buf.readContainerId();
        RecipeDisplay display = RecipeDisplay.read(buf, $ -> PacketUtils.readItem(buf));
        display.applyClientboundData(item -> {
            Optional<Item> remapped = itemManager.s2c(item, player);
            if (remapped.isEmpty()) {
                return item;
            }
            changed.set(true);
            return remapped.get();
        });

        if (changed.booleanValue()) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeContainerId(containerId);
            display.write(buf, ($, item) -> PacketUtils.writeItem(buf, item));
        }
    }
}
