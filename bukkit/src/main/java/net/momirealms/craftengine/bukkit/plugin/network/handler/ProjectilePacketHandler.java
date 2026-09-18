package net.momirealms.craftengine.bukkit.plugin.network.handler;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.entity.projectile.BukkitCustomProjectile;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.projectile.ProjectileDisplay;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ProjectilePacketHandler implements EntityPacketHandler {
    private final int entityId;
    private final BukkitCustomProjectile projectile;
    private final ProjectileDisplay display;

    public ProjectilePacketHandler(BukkitCustomProjectile projectile, ProjectileDisplay display, int entityId) {
        this.projectile = projectile;
        this.entityId = entityId;
        this.display = display;
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
    public void handleSyncEntityPosition(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
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
    public void handleMoveAndRotate(Player user, ByteBufPacketEvent event, int entityId, FriendlyByteBuf buf) {
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

    public void convertAddCustomProjectilePacket(FriendlyByteBuf buf, ByteBufPacketEvent event, Player user) {
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
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeVarInt(this.entityId);
        buf.writeUUID(uuid);
        buf.writeVarInt(EntityTypesProxy.ITEM_DISPLAY$registryId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        if (VersionHelper.isOrAbove1_21_9) buf.writeLpVec3(movement);
        buf.writeByte(MiscUtils.packDegrees(MiscUtils.clamp(-MiscUtils.unpackDegrees(xRot), -90.0F, 90.0F)));
        buf.writeByte(MiscUtils.packDegrees(-MiscUtils.unpackDegrees(yRot)));
        buf.writeByte(yHeadRot);
        buf.writeVarInt(data);
        if (!VersionHelper.isOrAbove1_21_9) {
            buf.writeShort(xa);
            buf.writeShort(ya);
            buf.writeShort(za);
        }
        user.sendPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(this.entityId, new ArrayList<>(4)), false);
    }

    public List<Object> createCustomProjectileEntityDataValues(Player player) {
        List<Object> itemDisplayValues = new ArrayList<>();
        Item displayedItem = Item.byId(this.display.item(), player);
        if (displayedItem == null) return itemDisplayValues;
        displayedItem = BukkitItemManager.instance().s2c(displayedItem, player, ItemPacketSource.ENTITY_DATA).orElse(displayedItem);

        // 我们应当使用新的展示物品的组件覆盖原物品的组件，以完成附魔，附魔光效等组件的继承.
        Item item = this.projectile.item();
        item.enchantments().ifPresent(displayedItem::setEnchantments);
        item.trim().ifPresent(displayedItem::trim);
        if (VersionHelper.isOrAbove1_20_5) {
            Optional<Boolean> glint = item.glint();
            if (glint.isPresent()) {
                displayedItem.glint(glint.get());
            }
        }

        DisplayData.ItemDisplayData.TransformationInterpolationDelay.addEntityDataIfNotDefaultValue(-1, itemDisplayValues);
        DisplayData.ItemDisplayData.Translation.addEntityDataIfNotDefaultValue(this.display.translation(), itemDisplayValues);
        DisplayData.ItemDisplayData.Scale.addEntityDataIfNotDefaultValue(this.display.scale(), itemDisplayValues);
        DisplayData.ItemDisplayData.LeftRotation.addEntityDataIfNotDefaultValue(this.display.rotation(), itemDisplayValues);
        if (VersionHelper.isOrAbove1_20_2) {
            DisplayData.ItemDisplayData.TransformationInterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
            DisplayData.ItemDisplayData.PosRotInterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
        } else {
            DisplayData.ItemDisplayData.InterpolationDuration.addEntityDataIfNotDefaultValue(1, itemDisplayValues);
        }

        DisplayData.ItemDisplayData.ItemStack.addEntityDataIfNotDefaultValue(displayedItem.minecraftItem(), itemDisplayValues);
        DisplayData.ItemDisplayData.ItemTransform.addEntityDataIfNotDefaultValue(this.display.displayType().id(), itemDisplayValues);
        DisplayData.ItemDisplayData.BillboardConstraints.addEntityDataIfNotDefaultValue(this.display.billboard().id(), itemDisplayValues);
        return itemDisplayValues;
    }
}
