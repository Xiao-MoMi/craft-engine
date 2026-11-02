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
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.concurrent.TimeUnit;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.MAIN_HAND_DYE;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.OFF_HAND_DYE;

public final class GeyserHeldDye extends ChannelDuplexHandler {
    private final GeyserSession session;
    private final HandProperties main;
    private final HandProperties off;
    private int mainColor = Integer.MIN_VALUE;
    private int offColor = Integer.MIN_VALUE;

    private GeyserHeldDye(GeyserSession session, HandProperties main, HandProperties off) {
        this.session = session;
        this.main = main;
        this.off = off;
    }

    public record HandProperties(GeyserIntEntityProperty red, GeyserIntEntityProperty greenBlue) {

        public static HandProperties register(GeyserDefineEntityPropertiesEvent event, boolean offhand) {
            String name = offhand ? OFF_HAND_DYE : MAIN_HAND_DYE;
            Identifier player = Identifier.of("minecraft:player");
            // 将 RGB 拆成 8 位与 16 位，两项都处于基岩实体整型属性的精确范围内。
            return new HandProperties(event.registerIntegerProperty(player, Identifier.of(name + "_r"), -1, 255, -1),
                    event.registerIntegerProperty(player, Identifier.of(name + "_gb"), 0, 65535, 0));
        }

        void send(PlayerEntity player, int rgb) {
            player.updatePropertiesBatched(batch -> {
                batch.update(this.red, rgb < 0 ? -1 : rgb >>> 16);
                batch.update(this.greenBlue, rgb < 0 ? 0 : rgb & 65535);
            }, true);
        }
    }

    public static void install(GeyserSession session, HandProperties main, HandProperties off) {
        GeyserHeldDye handler = new GeyserHeldDye(session, main, off);
        session.getUpstream().getSession().getPeer().getChannel().pipeline()
                .addBefore(session.getTickEventLoop(), BedrockPeer.NAME, "craftengine-held-dye", handler);
        session.executeInEventLoop(handler::syncOwnHands);
    }

    private void syncOwnHands() {
        if (this.session.isClosed()) {
            return;
        }
        if (this.session.isSpawned()) {
            PlayerInventory inventory = this.session.getPlayerInventory();
            int main = color(inventory.getItemInHand());
            int off = color(inventory.getOffhand());
            if (this.mainColor != main) {
                this.main.send(this.session.getPlayerEntity(), main);
                this.mainColor = main;
            }
            if (this.offColor != off) {
                this.off.send(this.session.getPlayerEntity(), off);
                this.offColor = off;
            }
        } else {
            this.mainColor = Integer.MIN_VALUE;
            this.offColor = Integer.MIN_VALUE;
        }
        this.session.scheduleInEventLoop(this::syncOwnHands, 50, TimeUnit.MILLISECONDS);
    }

    private static int color(GeyserItemStack item) {
        Integer color = item == null || item.isEmpty() ? null : item.getComponent(DataComponentTypes.DYED_COLOR);
        return color == null ? -1 : color & 0xffffff;
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

    private void syncRemote(long id, HandProperties hand, ItemData item) {
        if (id == this.session.getPlayerEntity().geyserId()) {
            return;
        }
        int color = item != null && item.getTag() != null && item.getTag().containsKey("customColor")
                ? item.getTag().getInt("customColor") & 0xffffff : -1;
        // 入场包先送出，再同步该实体的颜色。后续切换到未染色物品同样要清除旧值。
        this.session.executeInEventLoop(() -> {
            Entity entity = this.session.getEntityCache().getEntityByGeyserId(id);
            if (entity instanceof PlayerEntity player && player.isValid() && !this.session.isClosed()) {
                hand.send(player, color);
            }
        });
    }
}
