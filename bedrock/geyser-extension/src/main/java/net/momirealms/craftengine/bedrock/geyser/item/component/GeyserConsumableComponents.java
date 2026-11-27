package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponentGetter;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;

final class GeyserConsumableComponents {
    private static final float DEFAULT_USE_SPEED = 0.2f;
    private static final NbtMap EMPTY_FOOD = NbtMap.builder()
            .putInt("nutrition", 0)
            .putFloat("saturation_modifier", 0)
            .putBoolean("can_always_eat", true)
            .putCompound("using_converts_to", NbtMap.EMPTY).build();

    private GeyserConsumableComponents() {
    }

    static void tridentUse(NbtMapBuilder components, NbtMapBuilder properties, int useTicks) {
        // Geyser 用持续使用模拟三叉戟蓄力，但当前的动画映射表只有 SPEAR，没有 TRIDENT。
        components.putIfAbsent("minecraft:food", EMPTY_FOOD);
        components.putCompound("minecraft:use_animation", NbtMap.builder().putString("value", "spear").build());
        properties.putInt("use_animation", 6);
        useDuration(components, properties, useTicks / 20f);
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
            int nutrition = object.get("nutrition").getAsInt();
            food = NbtMap.builder().putInt("nutrition", nutrition)
                    .putFloat("saturation_modifier", saturationModifier(nutrition, object.get("saturation").getAsFloat()))
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

    private static float saturationModifier(int nutrition, float saturation) {
        // Java 存储恢复的饱和度点数，基岩按 nutrition × 2 × saturation_modifier 计算。
        return nutrition == 0 ? 0f : saturation / (2f * nutrition);
    }

    static GeyserItemComponentPatch nativeFood(String itemType, boolean inheritsFood) {
        return (data, mappings) -> {
            NbtMap components = data.getCompound("components");
            NbtMap food = components.getCompound("minecraft:food");
            if (food.isEmpty()) {
                return data;
            }
            NbtMap effectiveFood = inheritsFood ? inheritedFood(food, mappings, itemType) : food;
            boolean useFoodDuration = !effectiveFood.getBoolean("can_always_eat") && canUseFoodDuration(components);
            if (effectiveFood == food && !useFoodDuration) {
                return data;
            }
            NbtMapBuilder converted = components.toBuilder();
            converted.putCompound("minecraft:food", effectiveFood);
            if (useFoodDuration) {
                // food 按饱食度决定能否开始，item_properties.use_duration 继续以 tick 计时。
                converted.remove("minecraft:use_modifiers");
            }
            return data.toBuilder().putCompound("components", converted.build()).build();
        };
    }

    private static boolean canUseFoodDuration(NbtMap components) {
        String animation = components.getCompound("minecraft:use_animation").getString("value");
        NbtMap modifiers = components.getCompound("minecraft:use_modifiers");
        return (animation.equals("eat") || animation.equals("drink"))
                && !modifiers.isEmpty() && components.getCompound("item_properties").containsKey("use_duration")
                && modifiers.getFloat("movement_modifier", DEFAULT_USE_SPEED) == DEFAULT_USE_SPEED
                && modifiers.getBoolean("emit_vibrations", true) && !modifiers.containsKey("start_sound")
                && !components.containsKey("minecraft:shooter") && !components.containsKey("minecraft:throwable")
                && !components.containsKey("minecraft:kinetic_weapon");
    }

    private static NbtMap inheritedFood(NbtMap food, ItemMappings mappings, String itemType) {
        // 旧注册计划可能省略继承的 food；从 Java 原型取饱和度，避免重复换算基岩倍率。
        if (food.getInt("nutrition") == 0 && food.getFloat("saturation_modifier") == 0f) {
            return food;
        }
        ItemMapping mapping = mappings.getMapping(itemType);
        FoodProperties defaults = mapping == null ? null : mapping.getJavaItem().getComponent(ResolvableComponentGetter.EMPTY, DataComponentTypes.FOOD);
        if (defaults == null) {
            return food;
        }
        float modifier = saturationModifier(defaults.getNutrition(), defaults.getSaturationModifier());
        return food.getFloat("saturation_modifier") == modifier ? food : food.toBuilder().putFloat("saturation_modifier", modifier).build();
    }

    static GeyserItemComponentPatch useEffects(JsonElement value) {
        float speed = DEFAULT_USE_SPEED;
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
                components.putCompound("minecraft:use_modifiers", existing.toBuilder().putFloat("movement_modifier", movement).putBoolean("emit_vibrations", emitVibrations).build());
            }
        });
    }

    private static void useDuration(NbtMapBuilder components, NbtMapBuilder properties, float seconds) {
        NbtMap existing = (NbtMap) components.getOrDefault("minecraft:use_modifiers", NbtMap.EMPTY);
        components.putCompound("minecraft:use_modifiers", existing.toBuilder()
                .putFloat("movement_modifier", existing.getFloat("movement_modifier", DEFAULT_USE_SPEED))
                .putFloat("use_duration", seconds).build());
        properties.putInt("use_duration", (int) (seconds * 20));
    }
}
