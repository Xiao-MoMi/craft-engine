package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

final class GeyserItemComponents {

    private GeyserItemComponents() {
    }

    static GeyserItemComponentPatch stackSize(JsonElement value) {
        int size = value.isJsonNull() ? 1 : Math.min(value.getAsInt(), 64);
        NbtMap component = NbtMap.builder().putByte("value", (byte) size).build();
        return update((components, properties) -> {
            components.putCompound("minecraft:max_stack_size", component);
            properties.putInt("max_stack_size", size);
        });
    }

    static GeyserItemComponentPatch durability(JsonElement value) {
        int maximum = value.isJsonNull() ? 0 : value.getAsInt();
        NbtMap component = null;
        if (maximum > 0) {
            component = NbtMap.builder().putInt("max_durability", maximum)
                    .putCompound("damage_chance", NbtMap.builder().putInt("min", 1).putInt("max", 1).build()).build();
        }
        return component("minecraft:durability", component);
    }

    static GeyserItemComponentPatch glint(JsonElement value) {
        Boolean glint = value.isJsonNull() ? null : value.getAsBoolean();
        NbtMap component = glint == null ? null : NbtMap.builder().putBoolean("value", glint).build();
        return update((components, properties) -> {
            set(components, "minecraft:glint", component);
            if (glint == null) {
                properties.remove("foil");
            } else {
                properties.putBoolean("foil", glint);
            }
        });
    }

    static GeyserItemComponentPatch enchantable(JsonElement value) {
        int amount = value.isJsonNull() ? 0 : value.getAsJsonObject().get("value").getAsInt();
        NbtMap component = amount > 0 ? NbtMap.builder().putString("slot", "all").putByte("value", (byte) amount).build() : null;
        return update((components, properties) -> {
            set(components, "minecraft:enchantable", component);
            if (amount > 0) {
                properties.putString("enchantable_slot", "all");
                properties.putInt("enchantable_value", amount);
            } else {
                properties.remove("enchantable_slot");
                properties.remove("enchantable_value");
            }
        });
    }

    static GeyserItemComponentPatch equippable(JsonElement value) {
        if (value.isJsonNull()) {
            return component("minecraft:wearable", null);
        }
        String slot = switch (value.getAsJsonObject().get("slot").getAsString()) {
            case "feet" -> "slot.armor.feet";
            case "legs" -> "slot.armor.legs";
            case "chest" -> "slot.armor.chest";
            case "head" -> "slot.armor.head";
            case "body" -> "slot.armor";
            case "saddle" -> "slot.saddle";
            default -> null;
        };
        return update((components, properties) -> {
            if (slot == null) {
                components.remove("minecraft:wearable");
            } else {
                NbtMap existing = (NbtMap) components.getOrDefault("minecraft:wearable", NbtMap.EMPTY);
                components.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", slot)
                        .putInt("protection", existing.getInt("protection")).build());
            }
        });
    }

    static GeyserItemComponentPatch cooldown(JsonElement value, String itemType) {
        if (value.isJsonNull()) {
            return component("minecraft:cooldown", null);
        }
        JsonObject object = value.getAsJsonObject();
        String group = object.has("cooldown_group") ? object.get("cooldown_group").getAsString() : itemType;
        return component("minecraft:cooldown", NbtMap.builder().putString("category", group)
                .putFloat("duration", object.get("seconds").getAsFloat()).build());
    }

    static GeyserItemComponentPatch tool(JsonElement value) {
        boolean canDestroy = value.isJsonNull() || !value.getAsJsonObject().has("can_destroy_blocks_in_creative")
                || value.getAsJsonObject().get("can_destroy_blocks_in_creative").getAsBoolean();
        NbtMap component = NbtMap.builder().putBoolean("value", canDestroy).build();
        return update((components, properties) -> {
            components.putCompound("minecraft:can_destroy_in_creative", component);
            properties.putBoolean("can_destroy_in_creative", canDestroy);
        });
    }

    static GeyserItemComponentPatch repairable(JsonElement value) {
        NbtMap component = null;
        if (!value.isJsonNull()) {
            // 客户端只开放修复槽，材料和修复量仍由 Java 服务端校验。
            component = NbtMap.builder().putList("repair_items", NbtType.COMPOUND, NbtMap.builder()
                    .putList("items", NbtType.COMPOUND, NbtMap.builder().putString("tags", "1").build())
                    .putFloat("repair_amount", 1f).build()).build();
        }
        return component("minecraft:repairable", component);
    }

    static GeyserItemComponentPatch swing(JsonElement value) {
        if (value.isJsonNull()) {
            return component("minecraft:swing_duration", null);
        }
        JsonObject object = value.getAsJsonObject();
        int ticks = object.has("duration") ? object.get("duration").getAsInt() : 6;
        return component("minecraft:swing_duration", NbtMap.builder().putFloat("value", ticks / 20f).build());
    }

    static GeyserItemComponentPatch compostable(JsonElement value) {
        return component("minecraft:compostable", value.isJsonNull() ? null : NbtMap.builder().putInt("composting_chance", 1).build());
    }

    static GeyserItemComponentPatch fuel(JsonElement value) {
        return component("minecraft:fuel", value.isJsonNull() ? null : NbtMap.builder().putFloat("duration", 1f).build());
    }

    static GeyserItemComponentPatch component(String key, @Nullable NbtMap value) {
        return update((components, properties) -> set(components, key, value));
    }

    static void set(NbtMapBuilder components, String key, @Nullable NbtMap value) {
        if (value == null) {
            components.remove(key);
        } else {
            components.putCompound(key, value);
        }
    }

    static GeyserItemComponentPatch update(BiConsumer<NbtMapBuilder, NbtMapBuilder> updater) {
        return (data, mappings) -> {
            NbtMap original = data.getCompound("components");
            NbtMapBuilder components = original.toBuilder();
            NbtMapBuilder properties = original.getCompound("item_properties").toBuilder();
            updater.accept(components, properties);
            components.putCompound("item_properties", properties.build());
            return data.toBuilder().putCompound("components", components.build()).build();
        };
    }
}
