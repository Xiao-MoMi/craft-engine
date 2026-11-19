package net.momirealms.craftengine.bedrock.geyser.entity.display;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.ItemMappingsProxy;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.Registry;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.Collection;
import java.util.Map;

final class GeyserHeldItemTypes {
    static final GeyserHeldItemTypes EMPTY = new GeyserHeldItemTypes(0, 0);

    private final int firstId;
    private final ItemDefinition[] definitions;
    private final BedrockHeldItemType[] types;

    private GeyserHeldItemTypes(int firstId, int size) {
        this.firstId = firstId;
        this.definitions = new ItemDefinition[size];
        this.types = new BedrockHeldItemType[size];
    }

    @SuppressWarnings("unchecked")
    static Int2ObjectMap<GeyserHeldItemTypes> load() {
        // Geyser 服务端会重定位 fastutil，初始化时通过 Java Map 读取协议注册表
        Map<Integer, ItemMappings> mappings = (Map<Integer, ItemMappings>) ((Registry<?>) Registries.ITEMS).get();
        Int2ObjectMap<GeyserHeldItemTypes> tables = new Int2ObjectOpenHashMap<>(mappings.size());
        for (Map.Entry<Integer, ItemMappings> entry : mappings.entrySet()) {
            Map<?, ItemDefinition> definitions = (Map<?, ItemDefinition>) ItemMappingsProxy.INSTANCE.itemDefinitions(entry.getValue());
            tables.put(entry.getKey().intValue(), fromDefinitions(definitions.values()));
        }
        return tables;
    }

    static GeyserHeldItemTypes fromDefinitions(Collection<ItemDefinition> definitions) {
        int firstId = Integer.MAX_VALUE;
        int lastId = Integer.MIN_VALUE;
        int vanillaCount = 0;
        for (ItemDefinition definition : definitions) {
            if (definition.getIdentifier().startsWith("minecraft:")) {
                int id = definition.getRuntimeId();
                firstId = Math.min(firstId, id);
                lastId = Math.max(lastId, id);
                vanillaCount++;
            }
        }
        long span = (long) lastId - firstId + 1;
        if (vanillaCount == 0 || span > 2L * vanillaCount) {
            return EMPTY;
        }

        GeyserHeldItemTypes table = new GeyserHeldItemTypes(firstId, (int) span);
        for (ItemDefinition definition : definitions) {
            if (definition.getIdentifier().startsWith("minecraft:")) {
                int index = definition.getRuntimeId() - firstId;
                table.definitions[index] = definition;
                table.types[index] = classify(definition);
            }
        }
        return table;
    }

    BedrockHeldItemType resolve(ItemDefinition definition, boolean block) {
        if (block) {
            return BedrockHeldItemType.BLOCK;
        }
        int index = definition.getRuntimeId() - this.firstId;
        // 同一个 runtime ID 可能被会话或其他扩展替换，只有原注册对象可以直接复用分类
        if (index >= 0 && index < this.definitions.length && this.definitions[index] == definition) {
            return this.types[index];
        }
        return classify(definition);
    }

    private static BedrockHeldItemType classify(ItemDefinition definition) {
        String id = definition.getIdentifier();
        return switch (id) {
            case "minecraft:bow" -> BedrockHeldItemType.BOW;
            case "minecraft:shield" -> BedrockHeldItemType.SHIELD;
            case "minecraft:trident" -> BedrockHeldItemType.TRIDENT;
            case "minecraft:mace" -> BedrockHeldItemType.HANDHELD;
            case "minecraft:crossbow", "minecraft:spyglass" -> BedrockHeldItemType.NATIVE;
            case "minecraft:fishing_rod" -> BedrockHeldItemType.FISHING_ROD;
            default -> classifyComponents(definition, id);
        };
    }

    private static BedrockHeldItemType classifyComponents(ItemDefinition definition, String id) {
        if (id.startsWith("minecraft:") && id.endsWith("_spear")) {
            return BedrockHeldItemType.SPEAR;
        }
        NbtMap data = definition.getComponentData();
        if (data != null) {
            NbtMap components = data.getCompound("components");
            NbtMap properties = components.getCompound("item_properties");
            if (properties.containsKey("hand_equipped")) {
                return properties.getBoolean("hand_equipped") ? BedrockHeldItemType.HANDHELD : BedrockHeldItemType.SPRITE;
            }
            if (components.containsKey("minecraft:hand_equipped")) {
                return components.getCompound("minecraft:hand_equipped").getBoolean("value") ? BedrockHeldItemType.HANDHELD : BedrockHeldItemType.SPRITE;
            }
        }
        // Geyser 的组件表缺少部分旧物品，客户端仍用内置的 hand_equipped 分支渲染这些工具
        return id.endsWith("_sword") || id.endsWith("_axe") || id.endsWith("_pickaxe") || id.endsWith("_shovel") || id.endsWith("_hoe")
                ? BedrockHeldItemType.HANDHELD : BedrockHeldItemType.SPRITE;
    }
}
