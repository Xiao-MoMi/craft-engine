package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonObject;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.ItemComponentPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserSpearComponents extends ChannelDuplexHandler {
    private final Map<String, JsonObject> spears;
    private final ItemMappings mappings;

    private GeyserSpearComponents(Map<String, JsonObject> spears, ItemMappings mappings) {
        this.spears = Map.copyOf(spears);
        this.mappings = mappings;
    }

    public static void install(GeyserSession session, Map<String, JsonObject> spears) {
        session.getUpstream().getSession().getPeer().getChannel().pipeline()
                .addBefore(BedrockPeer.NAME, "craftengine-spear-components", new GeyserSpearComponents(spears, session.getItemMappings()));
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof BedrockPacketWrapper wrapper && wrapper.getPacket() instanceof ItemComponentPacket packet) {
            List<ItemDefinition> definitions = packet.getItems();
            for (int i = 0; i < definitions.size(); i++) {
                ItemDefinition definition = definitions.get(i);
                JsonObject entry = this.spears.get(definition.getIdentifier());
                if (entry != null) {
                    ItemDefinition vanilla = this.mappings.getDefinition(entry.get(ITEM_TYPE).getAsString());
                    NbtMap data = patch(definition.getComponentData(), vanilla.getComponentData(), entry.getAsJsonObject(CLIENT_BEHAVIOR));
                    // 只替换发给客户端的组件数据，保留 Geyser 注册对象、runtime ID 和栈身份。
                    definitions.set(i, new SimpleItemDefinition(definition.getIdentifier(), definition.getRuntimeId(),
                            definition.getVersion(), definition.isComponentBased(), data));
                }
            }
        }
        super.write(context, message, promise);
    }

    static NbtMap patch(NbtMap data, NbtMap vanilla, JsonObject behavior) {
        NbtMap original = data.getCompound("components");
        NbtMap nativeComponents = vanilla.getCompound("components");
        NbtMapBuilder components = original.toBuilder();
        List<String> tags = new ArrayList<>(original.getList("item_tags", NbtType.STRING));
        if (!tags.contains("minecraft:is_spear")) {
            tags.add("minecraft:is_spear");
        }
        components.putList("item_tags", NbtType.STRING, tags);
        if (behavior.has("kinetic")) {
            JsonObject kinetic = behavior.getAsJsonObject("kinetic");
            NbtMap existing = original.getCompound("minecraft:kinetic_weapon");
            if (existing.containsKey("minecraft:kinetic_weapon")) {
                existing = existing.getCompound("minecraft:kinetic_weapon");
            }
            NbtMapBuilder weapon = existing.toBuilder();
            weapon.putShort("delay", (short) kinetic.get("delayTicks").getAsInt());
            writeCondition(weapon, "dismount_conditions", kinetic.getAsJsonObject("dismount"));
            writeCondition(weapon, "knockback_conditions", kinetic.getAsJsonObject("knockback"));
            writeCondition(weapon, "damage_conditions", kinetic.getAsJsonObject("damage"));
            NbtMap fixed = weapon.build();
            // 客户端读取的是协议对应的组件封装；Geyser 当前输出的平铺结构不会被识别。
            if (nativeComponents.getCompound("minecraft:kinetic_weapon").containsKey("minecraft:kinetic_weapon")) {
                fixed = NbtMap.builder().putCompound("minecraft:kinetic_weapon", fixed).build();
            }
            components.putCompound("minecraft:kinetic_weapon", fixed);
            JsonObject use = behavior.getAsJsonObject("use");
            if (use.get("animation").getAsString().equals("SPEAR")) {
                // kinetic_weapon 本身不启动右键使用，补齐原版入口并保留已转换的移动、音效等设置。
                NbtMapBuilder modifiers = nativeComponents.getCompound("minecraft:use_modifiers").toBuilder();
                modifiers.putAll(original.getCompound("minecraft:use_modifiers"));
                int ticks = use.getAsJsonObject("maximumDuration").get("value").getAsInt();
                modifiers.putFloat("use_duration", ticks / 20f);
                components.putCompound("minecraft:use_modifiers", modifiers.build());
                components.putCompound("item_properties", original.getCompound("item_properties").toBuilder().putInt("use_duration", ticks).build());
            }
        }
        return data.toBuilder().putCompound("components", components.build()).build();
    }

    private static void writeCondition(NbtMapBuilder weapon, String key, JsonObject condition) {
        if (condition == null) {
            weapon.remove(key);
            return;
        }
        weapon.putCompound(key, NbtMap.builder()
                .putShort("max_duration", (short) condition.get("durationTicks").getAsInt())
                .putFloat("min_speed", condition.get("minimumSpeed").getAsFloat())
                .putFloat("min_relative_speed", condition.get("minimumRelativeSpeed").getAsFloat()).build());
    }
}
