package net.momirealms.craftengine.bedrock.geyser.item;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;

import java.util.concurrent.TimeUnit;

public final class GeyserHeldDye extends ChannelDuplexHandler {
    private final GeyserSession session;
    private final GeyserDyeProperties main;
    private final GeyserDyeProperties off;
    private int mainColor = Integer.MIN_VALUE;
    private int offColor = Integer.MIN_VALUE;

    private GeyserHeldDye(GeyserSession session, GeyserDyeProperties main, GeyserDyeProperties off) {
        this.session = session;
        this.main = main;
        this.off = off;
    }

    public static void install(GeyserSession session, GeyserDyeProperties main, GeyserDyeProperties off) {
        GeyserHeldDye handler = new GeyserHeldDye(session, main, off);
        session.getUpstream().getSession().getPeer().getChannel().pipeline().addBefore(session.getTickEventLoop(), BedrockPeer.NAME, "craftengine-held-dye", handler);
        session.executeInEventLoop(handler::syncOwnHands);
    }

    private void syncOwnHands() {
        if (this.session.isClosed()) {
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
        this.session.scheduleInEventLoop(this::syncOwnHands, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getTargetSubClientId() == 0) {
            if (wrapper.getPacket() instanceof AddPlayerPacket player) {
                this.syncRemote(player.getRuntimeEntityId(), this.main, player.getHand());
            } else if (wrapper.getPacket() instanceof MobEquipmentPacket equipment) {
                int slot = equipment.getContainerId();
                if (slot == ContainerId.INVENTORY || slot == ContainerId.OFFHAND) {
                    this.syncRemote(equipment.getRuntimeEntityId(), slot == ContainerId.OFFHAND ? this.off : this.main, equipment.getItem());
                }
            }
        }
        super.write(context, message, promise);
    }

    private void syncRemote(long id, GeyserDyeProperties hand, ItemData item) {
        if (id == this.session.getPlayerEntity().geyserId()) {
            return;
        }
        int color = item != null && item.getTag() != null && item.getTag().containsKey("customColor") ? item.getTag().getInt("customColor") & 0xffffff : -1;
        // 入场包先送出，再同步该实体的颜色。后续切换到未染色物品同样要清除旧值。
        this.session.executeInEventLoop(() -> {
            Entity entity = this.session.getEntityCache().getEntityByGeyserId(id);
            if (entity instanceof PlayerEntity player && player.isValid() && !this.session.isClosed()) {
                player.updatePropertiesBatched(batch -> hand.write(batch, color), true);
            }
        });
    }
}
