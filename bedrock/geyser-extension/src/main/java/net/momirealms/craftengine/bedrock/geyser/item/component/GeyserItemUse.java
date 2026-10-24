package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class GeyserItemUse {
    private static final Set<String> SPEAR_ITEMS = Set.of(
            "minecraft:wooden_spear",
            "minecraft:stone_spear",
            "minecraft:copper_spear",
            "minecraft:iron_spear",
            "minecraft:golden_spear",
            "minecraft:diamond_spear",
            "minecraft:netherite_spear"
    );

    private GeyserItemUse() {
    }

    @Nullable
    static GeyserItemComponentPatch read(@Nullable JsonObject behavior, JsonObject components, String itemType) {
        if (behavior == null) {
            return null;
        }
        JsonObject use = behavior.getAsJsonObject("use");
        String animation = use.get("animation").getAsString();
        int ticks = use.getAsJsonObject("maximumDuration").get("value").getAsInt();
        boolean using = !animation.equals("NONE") || ticks > 0 || use.get("nativeUseOnRelease").getAsBoolean();
        boolean spearUse = animation.equals("SPEAR");
        boolean spearPose = spearUse || SPEAR_ITEMS.contains(itemType);
        JsonElement kinetic = components.get("minecraft:kinetic_weapon");
        Boolean kineticEnabled = kinetic == null ? null : !kinetic.isJsonNull();
        return (data, mappings) -> {
            NbtMap original = data.getCompound("components");
            NbtMapBuilder converted = original.toBuilder();
            NbtMapBuilder properties = original.getCompound("item_properties").toBuilder();
            // 标签决定玩家姿态；移除持续攻击组件不应连带移除原版长矛的持握姿态。
            List<String> tags = original.getList("item_tags", NbtType.STRING);
            if (spearPose && !tags.contains("minecraft:is_spear")) {
                List<String> updatedTags = new ArrayList<>(tags);
                updatedTags.add("minecraft:is_spear");
                converted.putList("item_tags", NbtType.STRING, updatedTags);
            }
            if (!using) {
                converted.remove("minecraft:use_modifiers");
                properties.remove("use_duration");
            } else {
                NbtMapBuilder modifiers = NbtMap.builder();
                boolean hasKinetic = kineticEnabled != null ? kineticEnabled : original.containsKey("minecraft:kinetic_weapon");
                if (spearUse && hasKinetic) {
                    // 仅借用当前协议的使用入口，武器阶段数据只由 kinetic_weapon 转换器生成。
                    String templateType = SPEAR_ITEMS.contains(itemType) ? itemType : "minecraft:iron_spear";
                    ItemDefinition template = mappings.getDefinition(templateType);
                    if (template != null) {
                        modifiers.putAll(template.getComponentData().getCompound("components").getCompound("minecraft:use_modifiers"));
                    }
                }
                modifiers.putAll(original.getCompound("minecraft:use_modifiers"));
                if (!modifiers.isEmpty()) {
                    modifiers.putFloat("use_duration", ticks / 20f);
                    converted.putCompound("minecraft:use_modifiers", modifiers.build());
                    properties.putInt("use_duration", ticks);
                }
            }
            converted.putCompound("item_properties", properties.build());
            return data.toBuilder().putCompound("components", converted.build()).build();
        };
    }
}
