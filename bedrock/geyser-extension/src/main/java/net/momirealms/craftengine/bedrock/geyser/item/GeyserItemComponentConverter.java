package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.java.*;
import org.geysermc.geyser.api.util.Holders;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.util.Unit;

import java.util.List;
import java.util.Locale;
import java.util.Set;

final class GeyserItemComponentConverter {
    private static final Set<String> SUPPORTED = Set.of(
            "minecraft:consumable",
            "minecraft:food",
            "minecraft:equippable",
            "minecraft:max_damage",
            "minecraft:max_stack_size",
            "minecraft:use_cooldown",
            "minecraft:enchantable",
            "minecraft:tool",
            "minecraft:repairable",
            "minecraft:enchantment_glint_override",
            "minecraft:attack_range",
            "minecraft:kinetic_weapon",
            "minecraft:piercing_weapon",
            "minecraft:swing_animation",
            "minecraft:attack_animation",
            "minecraft:use_effects",
            "minecraft:compostable",
            "minecraft:cooking_fuel"
    );

    private GeyserItemComponentConverter() {
    }

    static boolean supports(String key) {
        return SUPPORTED.contains(key);
    }

    static String targetKey(String key) {
        return key.equals("minecraft:swing_animation") ? "minecraft:attack_animation" : key;
    }

    static void apply(CustomItemDefinition.Builder builder,
                      String key,
                      JsonElement value,
                      int sourceVersion,
                      List<String> warnings) {
        switch (key) {
            case "minecraft:max_stack_size" -> builder.component(JavaItemDataComponents.MAX_STACK_SIZE, integer(value, 1, 99));
            case "minecraft:max_damage" -> builder.component(JavaItemDataComponents.MAX_DAMAGE, integer(value, 0, Integer.MAX_VALUE));
            case "minecraft:enchantment_glint_override" -> builder.component(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, value.getAsBoolean());
            case "minecraft:enchantable" -> builder.component(JavaItemDataComponents.ENCHANTABLE, integer(value.getAsJsonObject().get("value"), 0, Integer.MAX_VALUE));
            case "minecraft:consumable" -> {
                JsonObject object = value.getAsJsonObject();
                String name = object.has("animation") ? object.get("animation").getAsString() : "eat";
                if (name.equals("trident") || (name.equals("spear") && sourceVersion < 12111)) {
                    throw new IllegalArgumentException("The API consumable animation enum cannot express TRIDENT");
                }
                JavaConsumable.Animation animation = JavaConsumable.Animation.valueOf(name.toUpperCase(Locale.ROOT));
                float seconds = number(object, "consume_seconds", 1.6F, 0, Float.MAX_VALUE);
                if (seconds == 0) {
                    throw new IllegalArgumentException("Zero consume duration cannot be expressed by this API");
                }
                builder.component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.of(seconds, animation));
                warnFields(object, warnings, "sound", "has_consume_particles");
            }
            case "minecraft:food" -> {
                JsonObject object = value.getAsJsonObject();
                builder.component(JavaItemDataComponents.FOOD, JavaFoodProperties.of(
                        integer(object.get("nutrition"), 0, Integer.MAX_VALUE),
                        number(object, "saturation", 0, 0, Float.MAX_VALUE), bool(object, "can_always_eat", false)));
            }
            case "minecraft:equippable" -> {
                JsonObject object = value.getAsJsonObject();
                JavaEquippable.EquipmentSlot slot = JavaEquippable.EquipmentSlot.valueOf(object.get("slot").getAsString().toUpperCase(Locale.ROOT));
                builder.component(JavaItemDataComponents.EQUIPPABLE, JavaEquippable.of(slot));
                warnFields(object, warnings, "asset_id", "camera_overlay");
            }
            case "minecraft:use_cooldown" -> {
                JsonObject object = value.getAsJsonObject();
                JavaUseCooldown.Builder cooldown = JavaUseCooldown.builder();
                float seconds = number(object, "seconds", 0, 0, Float.MAX_VALUE);
                if (seconds == 0) {
                    throw new IllegalArgumentException("Cooldown seconds must be positive");
                }
                cooldown.seconds(seconds);
                if (object.has("cooldown_group")) {
                    cooldown.cooldownGroup(Identifier.of(object.get("cooldown_group").getAsString()));
                }
                builder.component(JavaItemDataComponents.USE_COOLDOWN, cooldown.build());
            }
            case "minecraft:tool" -> builder.component(JavaItemDataComponents.TOOL, JavaTool.of(bool(value.getAsJsonObject(), "can_destroy_blocks_in_creative", true)));
            case "minecraft:repairable" -> builder.component(JavaItemDataComponents.REPAIRABLE, JavaRepairable.of(holders(value.getAsJsonObject().get("items"))));
            case "minecraft:attack_range" -> {
                JsonObject object = value.getAsJsonObject();
                builder.component(JavaItemDataComponents.ATTACK_RANGE, JavaAttackRange.builder()
                        .minReach(number(object, "min_reach", 0, 0, 64))
                        .maxReach(number(object, "max_reach", 3, 0, 64))
                        .minCreativeReach(number(object, "min_creative_reach", 0, 0, 64))
                        .maxCreativeReach(number(object, "max_creative_reach", 5, 0, 64))
                        .hitboxMargin(number(object, "hitbox_margin", 0.3F, 0, 1)).build());
            }
            case "minecraft:kinetic_weapon" -> {
                JsonObject object = value.getAsJsonObject();
                JavaKineticWeapon.Builder kinetic = JavaKineticWeapon.builder()
                        .delayTicks(object.has("delay_ticks") ? integer(object.get("delay_ticks"), 0, Integer.MAX_VALUE) : 0);
                if (object.has("dismount_conditions")) {
                    JsonObject condition = object.getAsJsonObject("dismount_conditions");
                    kinetic.dismountConditions(JavaKineticWeapon.condition(
                            integer(condition.get("max_duration_ticks"), 0, Integer.MAX_VALUE),
                            number(condition, "min_speed", 0, -Float.MAX_VALUE, Float.MAX_VALUE),
                            number(condition, "min_relative_speed", 0, -Float.MAX_VALUE, Float.MAX_VALUE)));
                }
                builder.component(JavaItemDataComponents.KINETIC_WEAPON, kinetic.build());
            }
            case "minecraft:piercing_weapon" -> builder.component(JavaItemDataComponents.PIERCING_WEAPON, JavaPiercingWeapon.instance());
            case "minecraft:attack_animation", "minecraft:swing_animation" -> {
                JsonObject object = value.getAsJsonObject();
                int duration = object.has("duration") ? integer(object.get("duration"), 1, Integer.MAX_VALUE) : 6;
                builder.component(JavaItemDataComponents.ATTACK_ANIMATION, JavaSwingAnimation.of(duration));
                if (object.has("type") && !object.get("type").getAsString().equals("whack")) {
                    warnings.add("Attack animation type requires resource support; the API only receives duration");
                }
            }
            case "minecraft:use_effects" -> {
                JsonObject object = value.getAsJsonObject();
                builder.component(JavaItemDataComponents.USE_EFFECTS,
                        JavaUseEffects.of(number(object, "speed_multiplier", 0.2F, 0, 1)));
                if (object.has("can_sprint") && !object.get("can_sprint").getAsBoolean()) {
                    warnings.add("can_sprint cannot be expressed by the API");
                }
            }
            case "minecraft:compostable" -> builder.component(JavaItemDataComponents.COMPOSTABLE, Unit.INSTANCE);
            case "minecraft:cooking_fuel" -> builder.component(JavaItemDataComponents.COOKING_FUEL, Unit.INSTANCE);
            default -> throw new IllegalArgumentException("Unsupported component " + key);
        }
    }

    private static Holders holders(JsonElement value) {
        if (value.isJsonPrimitive()) {
            String id = value.getAsString();
            return id.startsWith("#") ? Holders.ofTag(Identifier.of(id.substring(1))) : Holders.of(Identifier.of(id));
        }
        Holders.Builder builder = Holders.builder();
        for (JsonElement item : value.getAsJsonArray()) {
            builder.with(Identifier.of(item.getAsString()));
        }
        return builder.build();
    }

    private static void warnFields(JsonObject object, List<String> warnings, String... fields) {
        for (String field : fields) {
            if (object.has(field)) {
                warnings.add(field + " is not translated by the API; retain the source resource requirement");
            }
        }
    }

    private static int integer(JsonElement value, int minimum, int maximum) {
        int result = value.getAsBigDecimal().intValueExact();
        if (result < minimum || result > maximum) {
            throw new IllegalArgumentException("Integer outside [" + minimum + ", " + maximum + "]: " + result);
        }
        return result;
    }

    private static float number(JsonObject object, String field, float fallback, float minimum, float maximum) {
        float result = object.has(field) ? object.get(field).getAsFloat() : fallback;
        if (!Float.isFinite(result) || result < minimum || result > maximum) {
            throw new IllegalArgumentException(field + " outside [" + minimum + ", " + maximum + "]: " + result);
        }
        return result;
    }

    private static boolean bool(JsonObject object, String field, boolean fallback) {
        return object.has(field) ? object.get(field).getAsBoolean() : fallback;
    }
}
