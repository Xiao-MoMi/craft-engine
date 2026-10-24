package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.CLIENT_BEHAVIOR;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.ITEM_TYPE;

public record GeyserSpearComponentPatch(String itemType, @Nullable NbtMap kinetic, @Nullable Integer useDuration) implements GeyserItemComponentPatch {

    public static GeyserSpearComponentPatch read(JsonObject entry) {
        String itemType = entry.get(ITEM_TYPE).getAsString();
        JsonObject behavior = entry.getAsJsonObject(CLIENT_BEHAVIOR);
        if (!behavior.has("kinetic")) {
            return new GeyserSpearComponentPatch(itemType, null, null);
        }
        JsonObject kinetic = behavior.getAsJsonObject("kinetic");
        NbtMapBuilder weapon = NbtMap.builder();
        weapon.putShort("delay", (short) kinetic.get("delayTicks").getAsInt());
        readCondition(weapon, "dismount_conditions", kinetic.getAsJsonObject("dismount"));
        readCondition(weapon, "knockback_conditions", kinetic.getAsJsonObject("knockback"));
        readCondition(weapon, "damage_conditions", kinetic.getAsJsonObject("damage"));
        JsonObject use = behavior.getAsJsonObject("use");
        Integer useDuration = null;
        if (use.get("animation").getAsString().equals("SPEAR")) {
            useDuration = use.getAsJsonObject("maximumDuration").get("value").getAsInt();
        }
        return new GeyserSpearComponentPatch(itemType, weapon.build(), useDuration);
    }

    @Override
    public NbtMap apply(NbtMap data, ItemMappings mappings) {
        ItemDefinition vanilla = mappings.getDefinition(this.itemType);
        if (vanilla == null) {
            return data;
        }
        NbtMap original = data.getCompound("components");
        NbtMap nativeComponents = vanilla.getComponentData().getCompound("components");
        NbtMapBuilder components = original.toBuilder();
        List<String> tags = new ArrayList<>(original.getList("item_tags", NbtType.STRING));
        if (!tags.contains("minecraft:is_spear")) {
            tags.add("minecraft:is_spear");
        }
        components.putList("item_tags", NbtType.STRING, tags);
        if (this.kinetic != null) {
            // 客户端读取的是协议对应的组件封装；Geyser 当前输出的平铺结构不会被识别。
            NbtMap fixed = GeyserKineticWeaponComponentPatch.merge(original.getCompound("minecraft:kinetic_weapon"),
                    nativeComponents.getCompound("minecraft:kinetic_weapon"), this.kinetic);
            components.putCompound("minecraft:kinetic_weapon", fixed);
            if (this.useDuration != null) {
                // kinetic_weapon 本身不启动右键使用，补齐原版入口并保留已转换的移动、音效等设置。
                NbtMapBuilder modifiers = nativeComponents.getCompound("minecraft:use_modifiers").toBuilder();
                modifiers.putAll(original.getCompound("minecraft:use_modifiers"));
                int ticks = this.useDuration;
                modifiers.putFloat("use_duration", ticks / 20f);
                components.putCompound("minecraft:use_modifiers", modifiers.build());
                components.putCompound("item_properties", original.getCompound("item_properties").toBuilder().putInt("use_duration", ticks).build());
            }
        }
        return data.toBuilder().putCompound("components", components.build()).build();
    }

    private static void readCondition(NbtMapBuilder weapon, String key, @Nullable JsonObject condition) {
        if (condition != null) {
            weapon.putCompound(key, NbtMap.builder()
                    .putShort("max_duration", (short) condition.get("durationTicks").getAsInt())
                    .putFloat("min_speed", condition.get("minimumSpeed").getAsFloat())
                    .putFloat("min_relative_speed", condition.get("minimumRelativeSpeed").getAsFloat()).build());
        }
    }
}
