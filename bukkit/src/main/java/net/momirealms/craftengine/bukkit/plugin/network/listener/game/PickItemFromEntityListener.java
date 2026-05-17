package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerGamePacketListenerImplProxy;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PickItemFromEntityListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new PickItemFromEntityListener();

    private PickItemFromEntityListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        BukkitServerPlayer player = (BukkitServerPlayer) user;
        if (player == null) return;
        FriendlyByteBuf buf = event.getBuffer();
        int entityId = buf.readVarInt();
        BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByInteractableEntityId(entityId);
        if (furniture == null) {
            return;
        }
        Location location = furniture.location();
        if (!player.canInteractPoint(LocationUtils.toVec3d(location), 16)) {
            return;
        }
        BukkitCraftEngine.instance().scheduler().platform().run(
                () -> handlePickItemFromEntityOnMainThread((BukkitServerPlayer) user, furniture, furniture.hitboxByEntityId(entityId)),
               location
        );
    }

    private static void handlePickItemFromEntityOnMainThread(BukkitServerPlayer player, BukkitFurniture furniture, FurnitureHitBox hitbox) {
        Item item = furniture.controller.getItemToPickup(player, hitbox);
        Object itemStack;
        if (item == null) {
            Key itemId = furniture.config().settings().itemId();
            if (itemId == null) return;
            BukkitItem wrappedItem = BukkitItemManager.instance().createWrappedItem(itemId, player);
            if (wrappedItem == null) return;
            itemStack = wrappedItem.minecraftItem();
        } else {
            itemStack = item.minecraftItem();
        }
        tryPickItem(player.platformPlayer(), itemStack, CraftEntityProxy.INSTANCE.getEntity(furniture.bukkitEntity()));
    }

    private static void tryPickItem(Player player, Object itemStack, Object entity) {
        if (VersionHelper.isOrAbove1_21_5) {
            ServerGamePacketListenerImplProxy.INSTANCE.tryPickItem(ServerPlayerProxy.INSTANCE.getConnection(CraftEntityProxy.INSTANCE.getEntity(player)), itemStack, null, entity, true);
        } else if (VersionHelper.isOrAbove1_21_4) {
            ServerGamePacketListenerImplProxy.INSTANCE.tryPickItem(ServerPlayerProxy.INSTANCE.getConnection(CraftEntityProxy.INSTANCE.getEntity(player)), itemStack);
        }
    }
}
