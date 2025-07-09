package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitCustomProjectile;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.injector.ProtectedFieldVisitor;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.network.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProjectilePacketHandler implements EntityPacketHandler {
    private final int entityId;
    private final BukkitCustomProjectile projectile;
    private final Object cachedPacket;
    private final List<Object> cachedData;

    public ProjectilePacketHandler(BukkitCustomProjectile projectile, int entityId) {
        this.projectile = projectile;
        this.entityId = entityId;
        this.cachedData = createCustomProjectileEntityDataValues();
        this.cachedPacket = FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId, this.cachedData);
    }

    @Override
    public void handleSetEntityData(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(id);
        FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(this.cachedData, buf);
    }

    @Override
    public void handleSyncEntityPosition(NetWorkUser user, NMSPacketEvent event, Object packet) {
        Object converted = convertCustomProjectilePositionSyncPacket(packet);
        event.replacePacket(converted);
    }

    @Override
    public void handleMoveAndRotate(NetWorkUser user, NMSPacketEvent event, Object packet) {
        int entityId = ProtectedFieldVisitor.get().field$ClientboundMoveEntityPacket$entityId(packet);
        event.replacePacket(FastNMS.INSTANCE.constructor$ClientboundBundlePacket(List.of(
                this.cachedPacket,
                convertCustomProjectileMovePacket(packet, entityId)
        )));
    }

    public void convertAddCustomProjectilePacket(FriendlyByteBuf buf, ByteBufPacketEvent event) {
        UUID uuid = buf.readUUID();
        buf.readVarInt(); // type
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        byte xRot = buf.readByte();
        byte yRot = buf.readByte();
        byte yHeadRot = buf.readByte();
        int data = buf.readVarInt();
        int xa = buf.readShort();
        int ya = buf.readShort();
        int za = buf.readShort();
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(this.entityId);
        buf.writeUUID(uuid);
        buf.writeVarInt(MEntityTypes.ITEM_DISPLAY$registryId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeByte(MCUtils.packDegrees(MCUtils.clamp(-MCUtils.unpackDegrees(xRot), -90.0F, 90.0F)));
        buf.writeByte(MCUtils.packDegrees(-MCUtils.unpackDegrees(yRot)));
        buf.writeByte(yHeadRot);
        buf.writeVarInt(data);
        buf.writeShort(xa);
        buf.writeShort(ya);
        buf.writeShort(za);
    }

    private Object convertCustomProjectilePositionSyncPacket(Object packet) {
        int entityId = FastNMS.INSTANCE.method$ClientboundEntityPositionSyncPacket$id(packet);
        Object positionMoveRotation = FastNMS.INSTANCE.field$ClientboundEntityPositionSyncPacket$values(packet);
        boolean onGround = FastNMS.INSTANCE.field$ClientboundEntityPositionSyncPacket$onGround(packet);
        Object position = FastNMS.INSTANCE.field$PositionMoveRotation$position(positionMoveRotation);
        Object deltaMovement = FastNMS.INSTANCE.field$PositionMoveRotation$deltaMovement(positionMoveRotation);
        float yRot = FastNMS.INSTANCE.field$PositionMoveRotation$yRot(positionMoveRotation);
        float xRot = FastNMS.INSTANCE.field$PositionMoveRotation$xRot(positionMoveRotation);
        Object newPositionMoveRotation = FastNMS.INSTANCE.constructor$PositionMoveRotation(position, deltaMovement, -yRot, Math.clamp(-xRot, -90.0F, 90.0F));
        return FastNMS.INSTANCE.constructor$ClientboundEntityPositionSyncPacket(entityId, newPositionMoveRotation, onGround);
    }

    public List<Object> createCustomProjectileEntityDataValues() {
        List<Object> itemDisplayValues = new ArrayList<>();
        Optional<CustomItem<ItemStack>> customItem = BukkitItemManager.instance().getCustomItem(this.projectile.metadata().item());
        if (customItem.isEmpty()) return itemDisplayValues;
        ProjectileMeta meta = this.projectile.metadata();
        Item<ItemStack> displayedItem = customItem.get().buildItem(ItemBuildContext.EMPTY);
        // 我们应当使用新的展示物品的组件覆盖原物品的组件，以完成附魔，附魔光效等组件的继承
        displayedItem = this.projectile.item().mergeCopy(displayedItem);
        ItemDisplayEntityData.InterpolationDelay.addEntityDataIfNotDefaultValue(-1, itemDisplayValues);
        ItemDisplayEntityData.Translation.addEntityDataIfNotDefaultValue(meta.translation(), itemDisplayValues);
        ItemDisplayEntityData.Scale.addEntityDataIfNotDefaultValue(meta.scale(), itemDisplayValues);
        ItemDisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(meta.rotation(), itemDisplayValues);
        if (VersionHelper.isOrAbove1_20_2()) {
            ItemDisplayEntityData.TransformationInterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
            ItemDisplayEntityData.PositionRotationInterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
        } else {
            ItemDisplayEntityData.InterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
        }

        Object literalItem = displayedItem.getLiteralObject();
        BukkitItemManager.instance().s2c(displayedItem.getItem(), null).ifPresentOrElse(
                it -> ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(FastNMS.INSTANCE.field$CraftItemStack$handle(it), itemDisplayValues),
                () -> ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(literalItem, itemDisplayValues));
        ItemDisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(meta.displayType().id(), itemDisplayValues);
        ItemDisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(meta.billboard().id(), itemDisplayValues);
        return itemDisplayValues;
    }

    private Object convertCustomProjectileMovePacket(Object packet, int entityId) {
        short xa = FastNMS.INSTANCE.field$ClientboundMoveEntityPacket$xa(packet);
        short ya = FastNMS.INSTANCE.field$ClientboundMoveEntityPacket$ya(packet);
        short za = FastNMS.INSTANCE.field$ClientboundMoveEntityPacket$za(packet);
        float xRot = MCUtils.unpackDegrees(FastNMS.INSTANCE.field$ClientboundMoveEntityPacket$xRot(packet));
        float yRot = MCUtils.unpackDegrees(FastNMS.INSTANCE.field$ClientboundMoveEntityPacket$yRot(packet));
        boolean onGround = FastNMS.INSTANCE.field$ClientboundMoveEntityPacket$onGround(packet);
        return FastNMS.INSTANCE.constructor$ClientboundMoveEntityPacket$PosRot(
                entityId, xa, ya, za,
                MCUtils.packDegrees(-yRot), MCUtils.packDegrees(MCUtils.clamp(-xRot, -90.0F, 90.0F)),
                onGround
        );
    }
}
