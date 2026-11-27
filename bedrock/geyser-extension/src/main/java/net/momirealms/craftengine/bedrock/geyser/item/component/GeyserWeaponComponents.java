package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;

final class GeyserWeaponComponents {
    static final NbtMap DEFAULT_RANGE = NbtMap.builder()
            .putCompound("reach", NbtMap.builder().putFloat("min", 0).putFloat("max", 3).build())
            .putCompound("creative_reach", NbtMap.builder().putFloat("min", 0).putFloat("max", 5).build())
            .putFloat("hitbox_margin", 0.3f).build();

    private GeyserWeaponComponents() {
    }

    static GeyserItemComponentPatch piercing(JsonElement value) {
        if (value.isJsonNull()) {
            return GeyserItemComponents.component("minecraft:piercing_weapon", null);
        }
        return (data, mappings) -> {
            NbtMap original = data.getCompound("components");
            NbtMap range = original.getCompound("minecraft:piercing_weapon");
            if (range.isEmpty()) {
                range = original.getCompound("minecraft:kinetic_weapon");
                if (range.containsKey("minecraft:kinetic_weapon")) {
                    range = range.getCompound("minecraft:kinetic_weapon");
                }
            }
            if (range.containsKey("minecraft:piercing_weapon")) {
                range = range.getCompound("minecraft:piercing_weapon");
            }
            NbtMap weapon = NbtMap.builder()
                    .putCompound("reach", range.getCompound("reach", DEFAULT_RANGE.getCompound("reach")))
                    .putCompound("creative_reach", range.getCompound("creative_reach", DEFAULT_RANGE.getCompound("creative_reach")))
                    .putFloat("hitbox_margin", range.getFloat("hitbox_margin", 0.3f)).build();
            ItemDefinition vanilla = mappings.getDefinition("minecraft:iron_spear");
            if (vanilla != null && vanilla.getComponentData()
                    .getCompound("components")
                    .getCompound("minecraft:piercing_weapon")
                    .containsKey("minecraft:piercing_weapon")) {
                weapon = NbtMap.builder().putCompound("minecraft:piercing_weapon", weapon).build();
            }
            return data.toBuilder().putCompound("components", original.toBuilder()
                    .putCompound("minecraft:piercing_weapon", weapon).build()).build();
        };
    }

    static GeyserItemComponentPatch range(JsonElement value) {
        NbtMap range = DEFAULT_RANGE;
        if (!value.isJsonNull()) {
            JsonObject object = value.getAsJsonObject();
            range = NbtMap.builder()
                    .putCompound("reach", NbtMap.builder()
                            .putFloat("min", object.has("min_reach") ? object.get("min_reach").getAsFloat() : 0)
                            .putFloat("max", object.has("max_reach") ? object.get("max_reach").getAsFloat() : 3).build())
                    .putCompound("creative_reach", NbtMap.builder()
                            .putFloat("min", object.has("min_creative_reach") ? object.get("min_creative_reach").getAsFloat() : 0)
                            .putFloat("max", object.has("max_creative_reach") ? object.get("max_creative_reach").getAsFloat() : 5).build())
                    .putFloat("hitbox_margin", object.has("hitbox_margin") ? object.get("hitbox_margin").getAsFloat() : 0.3f).build();
        }
        NbtMap fields = range;
        return GeyserItemComponents.update((components, properties) -> {
            applyRange(components, "minecraft:kinetic_weapon", fields);
            applyRange(components, "minecraft:piercing_weapon", fields);
        });
    }

    private static void applyRange(NbtMapBuilder components, String key, NbtMap range) {
        NbtMap existing = (NbtMap) components.get(key);
        if (existing == null) {
            return;
        }
        boolean wrapped = existing.containsKey(key);
        NbtMapBuilder weapon = (wrapped ? existing.getCompound(key) : existing).toBuilder();
        weapon.putAll(range);
        NbtMap result = weapon.build();
        if (wrapped) {
            result = existing.toBuilder().putCompound(key, result).build();
        }
        components.putCompound(key, result);
    }
}
