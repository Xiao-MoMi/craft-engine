package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GeyserElytraEquipment {
    private final GeyserPlayer player;
    private final GeyserSession session;
    private final GeyserIntEntityProperty property;
    private final Map<String, Integer> variants;
    private final Map<PlayerEntity, Integer> sent = new IdentityHashMap<>();
    private ScheduledFuture<?> task;
    private ScheduledFuture<?> pending;

    GeyserElytraEquipment(GeyserPlayer player, GeyserIntEntityProperty property, Map<String, Integer> variants) {
        this.player = player;
        this.session = player.session();
        this.property = property;
        this.variants = variants;
    }

    public static GeyserElytraEquipment install(GeyserPlayer player, GeyserIntEntityProperty property, Map<String, Integer> variants) {
        GeyserElytraEquipment equipment = new GeyserElytraEquipment(player, property, variants);
        player.network().onSend(InventoryContentPacket.class, event -> equipment.nudge());
        player.network().onSend(InventorySlotPacket.class, event -> equipment.nudge());
        player.network().onSend(MobArmorEquipmentPacket.class, event -> equipment.nudge());
        player.network().onSend(AddPlayerPacket.class, event -> {
            long id = event.packet().getRuntimeEntityId();
            player.session().executeInEventLoop(() -> {
                equipment.sent.remove(player.session().getEntityCache().getEntityByGeyserId(id));
                equipment.nudge();
            });
        });
        player.session().executeInEventLoop(equipment::tick);
        return equipment;
    }

    public void close() {
        if (this.task != null) {
            this.task.cancel(false);
        }
        if (this.pending != null) {
            this.pending.cancel(false);
        }
        this.sent.clear();
    }

    private void nudge() {
        this.session.ensureInEventLoop(() -> {
            if (this.player.isClosed() || this.pending != null) {
                return;
            }
            this.pending = this.session.scheduleInEventLoop(() -> {
                this.pending = null;
                this.sync();
            }, 5, TimeUnit.MILLISECONDS);
        });
    }

    private void tick() {
        if (this.player.isClosed()) {
            return;
        }
        this.sync();
        this.task = this.session.scheduleInEventLoop(this::tick, 500, TimeUnit.MILLISECONDS);
    }

    void sync() {
        if (this.player.isClosed()) {
            return;
        }
        if (!this.session.isSpawned()) {
            this.sent.clear();
            return;
        }
        PlayerEntity self = this.session.getPlayerEntity();
        this.sync(self, this.session.getPlayerInventory().getItem(6));
        this.sent.keySet().removeIf(entity -> entity != self && !entity.isValid());
        this.session.getEntityCache().forEachPlayerEntity(entity -> {
            if (entity != self && entity.isValid()) {
                this.sync(entity, entity.getItemInSlot(EquipmentSlot.CHESTPLATE));
            }
        });
    }

    private void sync(PlayerEntity entity, GeyserItemStack chest) {
        // 必须从 Java 装备翻译款式；发出的胸部装备已被 Geyser 转为原版鞘翅以支持滑翔。
        int variant = chest == null || chest.isEmpty() ? 0 : this.variants.getOrDefault(chest.getItemData(this.session).getDefinition().getIdentifier(), 0);
        Integer previous = this.sent.get(entity);
        if (previous == null || previous != variant) {
            entity.updatePropertiesBatched(batch -> batch.update(this.property, variant), true);
            this.sent.put(entity, variant);
        }
    }
}
