package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GeyserHeldDye {
    private final GeyserPlayer player;
    private final GeyserSession session;
    private ScheduledFuture<?> task;
    private final GeyserDyeProperties main;
    private final GeyserDyeProperties off;
    private int mainColor = Integer.MIN_VALUE;
    private int offColor = Integer.MIN_VALUE;

    private GeyserHeldDye(GeyserPlayer player, GeyserDyeProperties main, GeyserDyeProperties off) {
        this.player = player;
        this.session = player.session();
        this.main = main;
        this.off = off;
    }

    public static GeyserHeldDye install(GeyserPlayer player, GeyserDyeProperties main, GeyserDyeProperties off) {
        GeyserHeldDye handler = new GeyserHeldDye(player, main, off);
        player.network().onSend(AddPlayerPacket.class, handler::onAddPlayer);
        player.network().onSend(MobEquipmentPacket.class, handler::onEquipment);
        player.session().executeInEventLoop(handler::syncOwnHands);
        return handler;
    }

    public void close() {
        if (this.task != null) {
            this.task.cancel(false);
        }
    }

    private void syncOwnHands() {
        if (this.player.isClosed()) {
            return;
        }
        if (this.session.isSpawned()) {
            PlayerInventory inventory = this.session.getPlayerInventory();
            int main = GeyserDyeProperties.color(inventory.getItemInHand());
            int off = GeyserDyeProperties.color(inventory.getOffhand());
            if (this.mainColor != main) {
                this.session.getPlayerEntity().updatePropertiesBatched(batch -> this.main.write(batch, main), true);
                this.mainColor = main;
            }
            if (this.offColor != off) {
                this.session.getPlayerEntity().updatePropertiesBatched(batch -> this.off.write(batch, off), true);
                this.offColor = off;
            }
        } else {
            this.mainColor = Integer.MIN_VALUE;
            this.offColor = Integer.MIN_VALUE;
        }
        this.task = this.session.scheduleInEventLoop(this::syncOwnHands, 50, TimeUnit.MILLISECONDS);
    }

    private void onAddPlayer(GeyserPacketEvent<AddPlayerPacket> event) {
        AddPlayerPacket player = event.packet();
        this.syncRemote(player.getRuntimeEntityId(), this.main, player.getHand());
    }

    private void onEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        int slot = equipment.getContainerId();
        if (slot == ContainerId.INVENTORY || slot == ContainerId.OFFHAND) {
            this.syncRemote(equipment.getRuntimeEntityId(), slot == ContainerId.OFFHAND ? this.off : this.main, equipment.getItem());
        }
    }

    private void syncRemote(long id, GeyserDyeProperties hand, ItemData item) {
        if (id == this.session.getPlayerEntity().geyserId()) {
            return;
        }
        int color = item != null && item.getTag() != null && item.getTag().containsKey("customColor") ? item.getTag().getInt("customColor") & 0xffffff : -1;
        // 入场包先送出，再同步该实体的颜色。后续切换到未染色物品同样要清除旧值。
        this.session.executeInEventLoop(() -> {
            Entity entity = this.session.getEntityCache().getEntityByGeyserId(id);
            if (entity instanceof PlayerEntity player && player.isValid() && !this.player.isClosed()) {
                player.updatePropertiesBatched(batch -> hand.write(batch, color), true);
            }
        });
    }
}
