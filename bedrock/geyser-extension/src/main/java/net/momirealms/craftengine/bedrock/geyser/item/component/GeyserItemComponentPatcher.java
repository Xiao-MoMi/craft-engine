package net.momirealms.craftengine.bedrock.geyser.item.component;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;

import java.util.List;
import java.util.Map;

public final class GeyserItemComponentPatcher extends ChannelDuplexHandler {
    private final Map<String, GeyserItemComponentPatch> patches;
    private final ItemMappings mappings;

    GeyserItemComponentPatcher(Map<String, GeyserItemComponentPatch> patches, ItemMappings mappings) {
        this.patches = patches;
        this.mappings = mappings;
    }

    public static void install(GeyserSession session, Map<String, GeyserItemComponentPatch> patches) {
        session.getUpstream().getSession().getPeer().getChannel().pipeline()
                .addBefore(BedrockPeer.NAME, "craftengine-item-components", new GeyserItemComponentPatcher(patches, session.getItemMappings()));
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getPacket() instanceof ItemComponentPacket packet) {
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
        super.write(context, message, promise);
    }
}
