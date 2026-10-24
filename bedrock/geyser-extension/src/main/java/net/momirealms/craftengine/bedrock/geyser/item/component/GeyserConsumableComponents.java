package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

final class GeyserConsumableComponents {
    private static final NbtMap EMPTY_FOOD = NbtMap.builder().putInt("nutrition", 0)
            .putFloat("saturation_modifier", 0).putBoolean("can_always_eat", true)
            .putCompound("using_converts_to", NbtMap.EMPTY).build();

    private GeyserConsumableComponents() {
    }

    static GeyserItemComponentPatch consumable(JsonElement value, int sourceVersion) {
        if (value.isJsonNull()) {
            return GeyserItemComponents.update((components, properties) -> {
                components.remove("minecraft:food");
                components.remove("minecraft:use_animation");
                properties.remove("use_animation");
                // 弓、投掷物和长矛仍有独立的使用入口，移除食用行为不能顺便清掉它们。
                if (components.containsKey("minecraft:shooter") || components.containsKey("minecraft:throwable")) {
                    useDuration(components, properties, 1000f);
                } else if (!components.containsKey("minecraft:kinetic_weapon")) {
                    components.remove("minecraft:use_modifiers");
                    properties.remove("use_duration");
                }
            });
        }
        JsonObject object = value.getAsJsonObject();
        String animation = object.has("animation") ? object.get("animation").getAsString() : "eat";
        if (animation.equals("trident") || (animation.equals("spear") && sourceVersion < 12111)) {
            animation = "spear";
        }
        int animationId = switch (animation) {
            case "none" -> 0;
            case "eat" -> 1;
            case "drink" -> 2;
            case "block" -> 3;
            case "bow" -> 4;
            case "spear" -> 6;
            case "crossbow" -> 9;
            case "spyglass" -> 10;
            case "brush" -> 12;
            default -> -1;
        };
        NbtMap useAnimation = NbtMap.builder().putString("value", animation).build();
        float seconds = object.has("consume_seconds") ? object.get("consume_seconds").getAsFloat() : 1.6f;
        return GeyserItemComponents.update((components, properties) -> {
            if (animationId < 0) {
                components.remove("minecraft:use_animation");
                properties.remove("use_animation");
            } else {
                components.putCompound("minecraft:use_animation", useAnimation);
                properties.putInt("use_animation", animationId);
            }
            components.putIfAbsent("minecraft:food", EMPTY_FOOD);
            useDuration(components, properties, seconds);
        });
    }

    static GeyserItemComponentPatch food(JsonElement value) {
        NbtMap food = EMPTY_FOOD;
        if (!value.isJsonNull()) {
            JsonObject object = value.getAsJsonObject();
            food = NbtMap.builder().putInt("nutrition", object.get("nutrition").getAsInt())
                    .putFloat("saturation_modifier", object.get("saturation").getAsFloat())
                    .putBoolean("can_always_eat", object.has("can_always_eat") && object.get("can_always_eat").getAsBoolean())
                    .putCompound("using_converts_to", NbtMap.EMPTY).build();
        }
        NbtMap converted = food;
        return GeyserItemComponents.update((components, properties) -> {
            // Java 的 food 只有营养信息；没有 consumable 时不能凭空添加食用入口。
            if (components.containsKey("minecraft:food")) {
                components.putCompound("minecraft:food", converted);
            }
        });
    }

    static GeyserItemComponentPatch useEffects(JsonElement value) {
        float speed = 0.2f;
        boolean vibrations = true;
        if (!value.isJsonNull()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("speed_multiplier")) {
                speed = object.get("speed_multiplier").getAsFloat();
            }
            if (object.has("interact_vibrations")) {
                vibrations = object.get("interact_vibrations").getAsBoolean();
            }
        }
        float movement = speed;
        boolean emitVibrations = vibrations;
        return GeyserItemComponents.update((components, properties) -> {
            NbtMap existing = (NbtMap) components.get("minecraft:use_modifiers");
            if (existing != null) {
                components.putCompound("minecraft:use_modifiers", existing.toBuilder()
                        .putFloat("movement_modifier", movement).putBoolean("emit_vibrations", emitVibrations).build());
            }
        });
    }

    private static void useDuration(NbtMapBuilder components, NbtMapBuilder properties, float seconds) {
        NbtMap existing = (NbtMap) components.getOrDefault("minecraft:use_modifiers", NbtMap.EMPTY);
        components.putCompound("minecraft:use_modifiers", existing.toBuilder()
                .putFloat("movement_modifier", existing.getFloat("movement_modifier", 0.2f))
                .putFloat("use_duration", seconds).build());
        properties.putInt("use_duration", (int) (seconds * 20));
    }
}
