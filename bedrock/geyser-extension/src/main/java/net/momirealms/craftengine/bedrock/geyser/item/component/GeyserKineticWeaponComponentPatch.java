package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.jetbrains.annotations.Nullable;

public record GeyserKineticWeaponComponentPatch(@Nullable NbtMap kinetic) implements GeyserItemComponentPatch {
    private static final String COMPONENT = "minecraft:kinetic_weapon";

    public static GeyserKineticWeaponComponentPatch read(JsonElement value) {
        if (value.isJsonNull()) {
            return new GeyserKineticWeaponComponentPatch(null);
        }
        JsonObject object = value.getAsJsonObject();
        NbtMapBuilder weapon = NbtMap.builder();
        weapon.putShort("delay", (short) (object.has("delay_ticks") ? object.get("delay_ticks").getAsInt() : 0));
        readCondition(weapon, "dismount_conditions", object.getAsJsonObject("dismount_conditions"));
        readCondition(weapon, "knockback_conditions", object.getAsJsonObject("knockback_conditions"));
        readCondition(weapon, "damage_conditions", object.getAsJsonObject("damage_conditions"));
        return new GeyserKineticWeaponComponentPatch(weapon.build());
    }

    @Override
    public NbtMap apply(NbtMap data, ItemMappings mappings) {
        NbtMap original = data.getCompound("components");
        NbtMapBuilder components = original.toBuilder();
        if (this.kinetic == null) {
            components.remove(COMPONENT);
        } else {
            // 任意基础物品都可以带 kinetic_weapon，使用原版长矛确定当前协议的封装。
            ItemDefinition vanilla = mappings.getDefinition("minecraft:iron_spear");
            if (vanilla == null) {
                return data;
            }
            NbtMap template = vanilla.getComponentData().getCompound("components").getCompound(COMPONENT);
            components.putCompound(COMPONENT, merge(original.getCompound(COMPONENT), template, this.kinetic));
        }
        return data.toBuilder().putCompound("components", components.build()).build();
    }

    static NbtMap merge(NbtMap existing, NbtMap template, NbtMap kinetic) {
        if (existing.containsKey(COMPONENT)) {
            existing = existing.getCompound(COMPONENT);
        }
        NbtMapBuilder weapon = GeyserWeaponComponents.DEFAULT_RANGE.toBuilder()
                .putFloat("damage_modifier", 0f)
                .putFloat("damage_multiplier", 1f);
        weapon.putAll(existing);
        weapon.remove("dismount_conditions");
        weapon.remove("knockback_conditions");
        weapon.remove("damage_conditions");
        weapon.putAll(kinetic);
        NbtMap fixed = weapon.build();
        if (template.containsKey(COMPONENT)) {
            fixed = NbtMap.builder().putCompound(COMPONENT, fixed).build();
        }
        return fixed;
    }

    private static void readCondition(NbtMapBuilder weapon, String key, @Nullable JsonObject condition) {
        if (condition != null) {
            weapon.putCompound(key, NbtMap.builder()
                    .putShort("max_duration", (short) condition.get("max_duration_ticks").getAsInt())
                    .putFloat("min_speed", condition.has("min_speed") ? condition.get("min_speed").getAsFloat() : 0)
                    .putFloat("min_relative_speed", condition.has("min_relative_speed") ? condition.get("min_relative_speed").getAsFloat() : 0)
                    .build());
        }
    }
}
