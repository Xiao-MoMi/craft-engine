package net.momirealms.craftengine.bedrock.geyser.item;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
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

    public void onInventoryContent(InventoryContentPacket packet) {
        if (packet.getContainerId() == ContainerId.ARMOR) {
            this.queue(this.session.getPlayerEntity().geyserId(), false);
        }
    }

    public void onInventorySlot(InventorySlotPacket packet) {
        if (packet.getContainerId() == ContainerId.ARMOR && packet.getSlot() == 1) {
            this.queue(this.session.getPlayerEntity().geyserId(), false);
        }
    }

    public void onRespawn(RespawnPacket packet) {
        if (packet.getState() == RespawnPacket.State.SERVER_READY) {
            this.queue(this.session.getPlayerEntity().geyserId(), true);
        }
    }

    public void remove(long id) {
        this.session.executeInEventLoop(() -> this.sent.remove(id));
    }

    public void close() {
        this.session.ensureInEventLoop(this.sent::clear);
    }

    public void queue(long id, boolean spawned) {
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
