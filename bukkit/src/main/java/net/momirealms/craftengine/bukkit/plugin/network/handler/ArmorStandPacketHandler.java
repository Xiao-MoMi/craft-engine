package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;

// entityViews 每个实体只能存一个处理器，装备 LOD 与实体数据替换需在同一个处理器里并存
public final class ArmorStandPacketHandler extends EquipmentEntityPacketHandler {
    public static final ArmorStandPacketHandler INSTANCE = new ArmorStandPacketHandler();

    private ArmorStandPacketHandler() {}

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        EntityDataPacketHandler.INSTANCE.handleSetEntityData(user, event);
    }
}
