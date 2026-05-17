package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitCustomProjectile;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ProjectilePacketHandler implements EntityPacketHandler {
    private final int entityId;
    private final BukkitCustomProjectile projectile;

    public ProjectilePacketHandler(BukkitCustomProjectile projectile, int entityId) {
        this.projectile = projectile;
        this.entityId = entityId;
    }

    @Override
    public void handleSetEntityData(Player user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(id);
        PacketUtils.clientboundSetEntityDataPacket$pack(this.createCustomProjectileEntityDataValues(user), buf);
    }

    @Override
    public void handleSyncEntityPosition(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        Vec3d position = buf.readVec3();
        Vec3d deltaMovement = buf.readVec3();
        float yRot = buf.readFloat();
        float xRot = buf.readFloat();
        boolean onGround = buf.readBoolean();
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(entityId);
        buf.writeVec3(position);
        buf.writeVec3(deltaMovement);
        buf.writeFloat(-yRot);
        buf.writeFloat(MiscUtils.clamp(-xRot, -90.0F, 90.0F));
        buf.writeBoolean(onGround);
    }

    @Override
    public void handleMoveAndRotate(NetWorkUser user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
        short xa = buf.readShort();
        short ya = buf.readShort();
        short za = buf.readShort();
        float yRot = MiscUtils.unpackDegrees(buf.readByte());
        float xRot = MiscUtils.unpackDegrees(buf.readByte());
        boolean onGround = buf.readBoolean();
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(entityId);
        buf.writeShort(xa);
        buf.writeShort(ya);
        buf.writeShort(za);
        buf.writeByte(MiscUtils.packDegrees(-yRot));
        buf.writeByte(MiscUtils.packDegrees(MiscUtils.clamp(-xRot, -90.0F, 90.0F)));
        buf.writeBoolean(onGround);
    }

    public void convertAddCustomProjectilePacket(FriendlyByteBuf buf, ByteBufPacketEvent event, NetWorkUser user) {
        UUID uuid = buf.readUUID();
        buf.readVarInt(); // type
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        Vec3d movement = VersionHelper.isOrAbove1_21_9 ? buf.readLpVec3() : null;
        byte xRot = buf.readByte();
        byte yRot = buf.readByte();
        byte yHeadRot = buf.readByte();
        int data = buf.readVarInt();
        int xa = VersionHelper.isOrAbove1_21_9 ? -1 : buf.readShort();
        int ya = VersionHelper.isOrAbove1_21_9 ? -1 : buf.readShort();
        int za = VersionHelper.isOrAbove1_21_9 ? -1 : buf.readShort();
        event.setCancelled(true);
        user.sendPackets(List.of(
                ClientboundAddEntityPacketProxy.INSTANCE.newInstance(this.entityId, uuid, x, y, z,
                        MiscUtils.clamp(-MiscUtils.unpackDegrees(xRot), -90.0F, 90.0F),
                        MiscUtils.packDegrees(-MiscUtils.unpackDegrees(yRot)),
                        EntityTypeProxy.ITEM_DISPLAY,
                        data,
                        movement != null ? Vec3Proxy.INSTANCE.newInstance(movement.x, movement.y, movement.z) : Vec3Proxy.INSTANCE.newInstance((double) xa / 8000.0, (double) ya / 8000.0, (double) za / 8000.0),
                        MiscUtils.unpackDegrees(yHeadRot)
                ),
                ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(entityId, this.createCustomProjectileEntityDataValues((Player) user))
        ), false);
    }

    public List<Object> createCustomProjectileEntityDataValues(Player player) {
        List<Object> itemDisplayValues = new ArrayList<>();
        Item displayedItem = BukkitItemManager.instance().createWrappedItem(this.projectile.metadata().item(), player);
        if (displayedItem == null) return itemDisplayValues;
        displayedItem = BukkitItemManager.instance().s2c(displayedItem, player).orElse(displayedItem);

        ProjectileMeta meta = this.projectile.metadata();

        // 我们应当使用新的展示物品的组件覆盖原物品的组件，以完成附魔，附魔光效等组件的继承.
        Item item = this.projectile.item();
        item.enchantments().ifPresent(displayedItem::setEnchantments);
        if (VersionHelper.isOrAbove1_20_5) {
            Optional<Boolean> glint = item.glint();
            if (glint.isPresent()) {
                displayedItem.glint(glint.get());
            }
        }

        DisplayData.ItemDisplayData.TransformationInterpolationDelay.addEntityDataIfNotDefaultValue(-1, itemDisplayValues);
        DisplayData.ItemDisplayData.Translation.addEntityDataIfNotDefaultValue(meta.translation(), itemDisplayValues);
        DisplayData.ItemDisplayData.Scale.addEntityDataIfNotDefaultValue(meta.scale(), itemDisplayValues);
        DisplayData.ItemDisplayData.LeftRotation.addEntityDataIfNotDefaultValue(meta.rotation(), itemDisplayValues);
        if (VersionHelper.isOrAbove1_20_2) {
            DisplayData.ItemDisplayData.TransformationInterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
            DisplayData.ItemDisplayData.PosRotInterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
        } else {
            DisplayData.ItemDisplayData.InterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
        }

        DisplayData.ItemDisplayData.ItemStack.addEntityDataIfNotDefaultValue(displayedItem.minecraftItem(), itemDisplayValues);
        DisplayData.ItemDisplayData.ItemTransform.addEntityDataIfNotDefaultValue(meta.displayType().id(), itemDisplayValues);
        DisplayData.ItemDisplayData.BillboardConstraints.addEntityDataIfNotDefaultValue(meta.billboard().id(), itemDisplayValues);
        return itemDisplayValues;
    }
}
