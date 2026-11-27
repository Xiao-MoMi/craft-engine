package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.entity.property.BatchPropertyUpdater;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GeyserHeldDye {
    private static final int JAVA_HELMET_SLOT = 5;
    private final GeyserPlayer player;
    private final GeyserSession session;
    private final AtomicBoolean handsQueued = new AtomicBoolean();
    private final Hand main;
    private final Hand off;
    private final GeyserDyeProperties head;
    private int headColor = Integer.MIN_VALUE;

    private GeyserHeldDye(GeyserPlayer player, GeyserDyeProperties main, GeyserDyeProperties off, GeyserDyeProperties mainThird, GeyserDyeProperties offThird, GeyserDyeProperties head) {
        this.player = player;
        this.session = player.session();
        this.main = new Hand(main, mainThird);
        this.off = new Hand(off, offThird);
        this.head = head;
    }

    public static GeyserHeldDye install(GeyserPlayer player, GeyserDyeProperties main, GeyserDyeProperties off, GeyserDyeProperties mainThird, GeyserDyeProperties offThird, GeyserDyeProperties head) {
        GeyserHeldDye handler = new GeyserHeldDye(player, main, off, mainThird, offThird, head);
        handler.queueHands();
        return handler;
    }

    public void onHotbar(PlayerHotbarPacket packet) {
        if (packet.isSelectHotbarSlot()) {
            this.queueHands();
        }
    }

    public void onInventoryContent(InventoryContentPacket packet) {
        int container = packet.getContainerId();
        if (container == ContainerId.INVENTORY || container == ContainerId.OFFHAND) {
            this.queueHands();
        } else if (container == ContainerId.ARMOR) {
            this.queueHead(this.session.getPlayerEntity().geyserId());
        }
    }

    public void onInventorySlot(InventorySlotPacket packet) {
        int container = packet.getContainerId();
        if (container == ContainerId.OFFHAND || (container == ContainerId.INVENTORY && packet.getSlot() == this.session.getPlayerInventory().getHeldItemSlot())) {
            this.queueHands();
        } else if (container == ContainerId.ARMOR && packet.getSlot() == 0) {
            this.queueHead(this.session.getPlayerEntity().geyserId());
        }
    }

    public void onRespawn(RespawnPacket packet) {
        if (packet.getState() == RespawnPacket.State.SERVER_READY) {
            this.main.reset();
            this.off.reset();
            this.headColor = Integer.MIN_VALUE;
            this.queueHands();
        }
    }

    public void queueHands() {
        if (this.handsQueued.compareAndSet(false, true)) {
            this.session.executeInEventLoop(this::syncOwnHands);
        }
    }

    private void syncOwnHands() {
        this.handsQueued.set(false);
        if (this.player.isClosed() || !this.session.isSpawned()) {
            return;
        }
        if (this.headColor == Integer.MIN_VALUE) {
            this.syncOwnHead();
        }
        PlayerInventory inventory = this.session.getPlayerInventory();
        GeyserItemStack mainStack = inventory.getItemInHand();
        GeyserItemStack offStack = inventory.getOffhand();
        GeyserItemRuntime runtime = this.player.items();
        GeyserItemTints mainTints = runtime == null ? GeyserItemTints.DEFAULT : runtime.tints(mainStack, true);
        GeyserItemTints offTints = runtime == null ? GeyserItemTints.DEFAULT : runtime.tints(offStack, false);
        this.main.sample(mainStack.isEmpty() ? null : mainStack::getComponent, mainTints.firstRight(), mainTints.thirdRight());
        this.off.sample(offStack.isEmpty() ? null : offStack::getComponent, offTints.firstLeft(), offTints.thirdLeft());
        if (this.main.changed() || this.off.changed()) {
            this.session.getPlayerEntity().updatePropertiesBatched(batch -> {
                this.main.writeChanges(batch);
                this.off.writeChanges(batch);
            }, true);
        }
    }

    public void queueHead(long id) {
        this.session.executeInEventLoop(() -> {
            if (this.player.isClosed()) {
                return;
            }
            if (id == this.session.getPlayerEntity().geyserId()) {
                if (this.session.isSpawned()) {
                    this.syncOwnHead();
                }
            } else if (this.session.getEntityCache().getEntityByGeyserId(id) instanceof PlayerEntity entity && entity.isValid()) {
                int color = this.resolveHeadColor(entity.getItemInSlot(EquipmentSlot.HELMET));
                entity.updatePropertiesBatched(batch -> this.head.write(batch, color), true);
            }
        });
    }

    private void syncOwnHead() {
        int color = this.resolveHeadColor(this.session.getPlayerInventory().getItem(JAVA_HELMET_SLOT));
        if (this.headColor != color) {
            this.session.getPlayerEntity().updatePropertiesBatched(batch -> this.head.write(batch, color), true);
            this.headColor = color;
        }
    }

    private int resolveHeadColor(GeyserItemStack stack) {
        GeyserItemRuntime runtime = this.player.items();
        if (runtime == null || stack.isEmpty()) {
            return -1;
        }
        return GeyserItemTints.color(stack, runtime.tints(stack, false).head());
    }

    public void onAddPlayer(GeyserPacketEvent<AddPlayerPacket> event) {
        AddPlayerPacket player = event.packet();
        this.syncRemote(player.getRuntimeEntityId(), false, player.getHand());
    }

    public void onEquipment(GeyserPacketEvent<MobEquipmentPacket> event) {
        MobEquipmentPacket equipment = event.packet();
        int slot = equipment.getContainerId();
        if (slot == ContainerId.INVENTORY || slot == ContainerId.OFFHAND) {
            if (equipment.getRuntimeEntityId() == this.session.getPlayerEntity().geyserId()) {
                this.queueHands();
            } else {
                this.syncRemote(equipment.getRuntimeEntityId(), slot == ContainerId.OFFHAND, equipment.getItem());
            }
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
                int color;
                if (stack.isEmpty()) {
                    color = fallback;
                } else {
                    GeyserItemRuntime runtime = this.player.items();
                    GeyserItemTints tints = runtime == null ? GeyserItemTints.DEFAULT : runtime.tints(stack, false);
                    color = GeyserItemTints.color(stack, offhand ? tints.thirdLeft() : tints.thirdRight());
                }
                Hand hand = offhand ? this.off : this.main;
                player.updatePropertiesBatched(batch -> {
                    hand.first.write(batch, color);
                    hand.third.write(batch, color);
                }, true);
            }
        });
    }

    static final class Hand {
        private final GeyserDyeProperties first;
        private final GeyserDyeProperties third;
        private int firstColor;
        private int thirdColor;
        private int sentFirst = Integer.MIN_VALUE;
        private int sentThird = Integer.MIN_VALUE;

        Hand(GeyserDyeProperties first, GeyserDyeProperties third) {
            this.first = first;
            this.third = third;
        }

        void sample(GeyserItemTints.ComponentGetter components, int firstSource, int thirdSource) {
            this.firstColor = components == null ? -1 : GeyserItemTints.color(components, firstSource);
            this.thirdColor = components == null || firstSource == thirdSource ? this.firstColor : GeyserItemTints.color(components, thirdSource);
        }

        boolean changed() {
            return this.firstColor != this.sentFirst || this.thirdColor != this.sentThird;
        }

        void writeChanges(BatchPropertyUpdater batch) {
            if (this.firstColor != this.sentFirst) {
                this.first.write(batch, this.firstColor);
                this.sentFirst = this.firstColor;
            }
            if (this.thirdColor != this.sentThird) {
                this.third.write(batch, this.thirdColor);
                this.sentThird = this.thirdColor;
            }
        }

        void reset() {
            this.sentFirst = Integer.MIN_VALUE;
            this.sentThird = Integer.MIN_VALUE;
        }
    }
}
