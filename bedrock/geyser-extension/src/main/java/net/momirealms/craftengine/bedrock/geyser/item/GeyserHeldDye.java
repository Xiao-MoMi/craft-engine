package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
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
    private final GeyserDyeProperties mainThird;
    private final GeyserDyeProperties offThird;
    private int mainThirdColor = Integer.MIN_VALUE;
    private int offThirdColor = Integer.MIN_VALUE;
    private int mainColor = Integer.MIN_VALUE;
    private int offColor = Integer.MIN_VALUE;

    private GeyserHeldDye(GeyserPlayer player, GeyserDyeProperties main, GeyserDyeProperties off, GeyserDyeProperties mainThird, GeyserDyeProperties offThird) {
        this.player = player;
        this.session = player.session();
        this.main = main;
        this.off = off;
        this.mainThird = mainThird;
        this.offThird = offThird;
    }

    public static GeyserHeldDye install(GeyserPlayer player, GeyserDyeProperties main, GeyserDyeProperties off, GeyserDyeProperties mainThird, GeyserDyeProperties offThird) {
        GeyserHeldDye handler = new GeyserHeldDye(player, main, off, mainThird, offThird);
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
            GeyserItemStack mainStack = inventory.getItemInHand();
            GeyserItemStack offStack = inventory.getOffhand();
            GeyserItemRuntime runtime = this.player.items();
            ItemData mainItem = runtime == null ? ItemData.AIR : runtime.handItem(mainStack, true);
            ItemData offItem = runtime == null ? ItemData.AIR : runtime.handItem(offStack, false);
            int main = runtime == null ? GeyserDyeProperties.color(mainStack) : runtime.handColor(mainStack, mainItem, false, true);
            int off = runtime == null ? GeyserDyeProperties.color(offStack) : runtime.handColor(offStack, offItem, true, true);
            int mainThird = runtime == null ? main : runtime.handColor(mainStack, mainItem, false, false);
            int offThird = runtime == null ? off : runtime.handColor(offStack, offItem, true, false);
            if (this.mainThirdColor != mainThird) {
                this.session.getPlayerEntity().updatePropertiesBatched(batch -> this.mainThird.write(batch, mainThird), true);
                this.mainThirdColor = mainThird;
            }
            if (this.offThirdColor != offThird) {
                this.session.getPlayerEntity().updatePropertiesBatched(batch -> this.offThird.write(batch, offThird), true);
                this.offThirdColor = offThird;
            }
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
            this.mainThirdColor = Integer.MIN_VALUE;
            this.offThirdColor = Integer.MIN_VALUE;
        }
        this.task = this.session.scheduleInEventLoop(this::syncOwnHands, 50, TimeUnit.MILLISECONDS);
    }

    private int color(GeyserItemStack stack, boolean offhand, boolean firstPerson, boolean selected) {
        GeyserItemRuntime runtime = this.player.items();
        return runtime == null ? GeyserDyeProperties.color(stack) : runtime.handColor(stack, offhand, firstPerson, selected);
    }

    private void onAddPlayer(GeyserPacketEvent<AddPlayerPacket> event) {
        AddPlayerPacket player = event.packet();
        this.syncRemote(player.getRuntimeEntityId(), false, player.getHand());
    }

    private void onEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        int slot = equipment.getContainerId();
        if (slot == ContainerId.INVENTORY || slot == ContainerId.OFFHAND) {
            this.syncRemote(equipment.getRuntimeEntityId(), slot == ContainerId.OFFHAND, equipment.getItem());
        }
    }

    private void syncRemote(long id, boolean offhand, ItemData item) {
        if (id == this.session.getPlayerEntity().geyserId()) {
            return;
        }
        int fallback = item != null && item.getTag() != null && item.getTag().containsKey("customColor") ? item.getTag().getInt("customColor") & 0xffffff : -1;
        // 入场包先送出，再同步该实体的颜色。后续切换到未染色物品同样要清除旧值。
        this.session.executeInEventLoop(() -> {
            Entity entity = this.session.getEntityCache().getEntityByGeyserId(id);
            if (entity instanceof PlayerEntity player && player.isValid() && !this.player.isClosed()) {
                GeyserItemStack stack = offhand ? player.getOffHandItem() : player.getMainHandItem();
                int color = stack.isEmpty() ? fallback : this.color(stack, offhand, false, false);
                GeyserDyeProperties hand = offhand ? this.off : this.main;
                GeyserDyeProperties third = offhand ? this.offThird : this.mainThird;
                player.updatePropertiesBatched(batch -> {
                    hand.write(batch, color);
                    third.write(batch, color);
                }, true);
            }
        });
    }
}
