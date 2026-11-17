package net.momirealms.craftengine.bedrock.geyser.item;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;

import java.util.Map;

public final class GeyserElytraEquipment {
    private final GeyserPlayer player;
    private final GeyserSession session;
    private final GeyserIntEntityProperty property;
    private final Map<String, Integer> variants;
    private final Long2ObjectOpenHashMap<Integer> sent = new Long2ObjectOpenHashMap<>();

    GeyserElytraEquipment(GeyserPlayer player, GeyserIntEntityProperty property, Map<String, Integer> variants) {
        this.player = player;
        this.session = player.session();
        this.property = property;
        this.variants = variants;
    }

    public static GeyserElytraEquipment install(GeyserPlayer player, GeyserIntEntityProperty property, Map<String, Integer> variants) {
        GeyserElytraEquipment equipment = new GeyserElytraEquipment(player, property, variants);
        long self = player.session().getPlayerEntity().geyserId();
        player.network().onSend(InventoryContentPacket.class, event -> {
            if (event.packet().getContainerId() == ContainerId.ARMOR) {
                equipment.queue(self, false);
            }
        });
        player.network().onSend(InventorySlotPacket.class, event -> {
            if (event.packet().getContainerId() == ContainerId.ARMOR && event.packet().getSlot() == 1) {
                equipment.queue(self, false);
            }
        });
        player.network().onSend(MobArmorEquipmentPacket.class, event -> equipment.queue(event.packet().getRuntimeEntityId(), false));
        player.network().onSend(AddPlayerPacket.class, event -> equipment.queue(event.packet().getRuntimeEntityId(), true));
        player.network().onSend(RespawnPacket.class, event -> {
            if (event.packet().getState() == RespawnPacket.State.SERVER_READY) {
                equipment.queue(self, true);
            }
        });
        player.network().onSend(RemoveEntityPacket.class, event -> {
            long id = event.packet().getUniqueEntityId();
            equipment.session.executeInEventLoop(() -> equipment.sent.remove(id));
        });
        return equipment;
    }

    public void close() {
        this.session.ensureInEventLoop(this.sent::clear);
    }

    private void queue(long id, boolean spawned) {
        this.session.executeInEventLoop(() -> {
            if (this.player.isClosed() || !this.session.isSpawned()) {
                return;
            }
            PlayerEntity self = this.session.getPlayerEntity();
            if (id == self.geyserId()) {
                this.sync(self, this.session.getPlayerInventory().getItem(6), spawned);
            } else if (this.session.getEntityCache().getEntityByGeyserId(id) instanceof PlayerEntity entity && entity.isValid()) {
                this.sync(entity, entity.getItemInSlot(EquipmentSlot.CHESTPLATE), spawned);
            }
        });
    }

    private void sync(PlayerEntity entity, GeyserItemStack chest, boolean spawned) {
        // 必须从 Java 装备翻译款式；发出的胸部装备已被 Geyser 转为原版鞘翅以支持滑翔。
        int variant = chest == null || chest.isEmpty() ? 0 : this.variants.getOrDefault(chest.getItemData(this.session).getDefinition().getIdentifier(), 0);
        Integer previous = this.sent.put(entity.geyserId(), Integer.valueOf(variant));
        if (spawned || previous == null || previous != variant) {
            entity.updatePropertiesBatched(batch -> batch.update(this.property, variant), true);
        }
    }
}
