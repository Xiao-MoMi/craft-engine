package net.momirealms.craftengine.bedrock.geyser.item.component;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.List;
import java.util.Map;

public final class GeyserItemComponentPatcher {
    private final Map<String, GeyserItemComponentPatch> patches;
    private final ItemMappings mappings;

    GeyserItemComponentPatcher(Map<String, GeyserItemComponentPatch> patches, ItemMappings mappings) {
        this.patches = patches;
        this.mappings = mappings;
    }

    public static void install(GeyserPlayer player, Map<String, GeyserItemComponentPatch> patches) {
        GeyserItemComponentPatcher patcher = new GeyserItemComponentPatcher(patches, player.session().getItemMappings());
        player.network().onSend(ItemComponentPacket.class, patcher::patch);
    }

    private void patch(GeyserPacketEvent<ItemComponentPacket> event) {
        ItemComponentPacket packet = event.packet();
        List<ItemDefinition> definitions = packet.getItems();
        for (int i = 0; i < definitions.size(); i++) {
            ItemDefinition definition = definitions.get(i);
            GeyserItemComponentPatch patch = this.patches.get(definition.getIdentifier());
            if (patch != null) {
                NbtMap original = definition.getComponentData();
                NbtMap data = patch.apply(original, this.mappings);
                if (data == original) {
                    continue;
                }
                // 只替换发给客户端的组件数据，保留 Geyser 注册对象、runtime ID 和栈身份。
                definitions.set(i, new SimpleItemDefinition(
                        definition.getIdentifier(),
                        definition.getRuntimeId(),
                        definition.getVersion(),
                        definition.isComponentBased(),
                        data
                ));
            }
        }
    }
}
