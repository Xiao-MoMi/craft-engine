package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.java.*;
import org.geysermc.geyser.api.util.Holders;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.util.Unit;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class GeyserItemComponentConverter {
    private static final Map<String, Converter> CONVERTERS = new HashMap<>();

    static {
        register(
                "minecraft:max_stack_size",
                JavaItemDataComponents.MAX_STACK_SIZE,
                (value, version) -> value.getAsInt()
        );
        register(
                "minecraft:max_damage",
                JavaItemDataComponents.MAX_DAMAGE,
                (value, version) -> value.getAsInt()
        );
        register(
                "minecraft:enchantment_glint_override",
                JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE,
                (value, version) -> value.getAsBoolean()
        );
        register(
                "minecraft:enchantable",
                JavaItemDataComponents.ENCHANTABLE,
                (value, version) -> value.getAsJsonObject().get("value").getAsInt()
        );
        register(
                "minecraft:consumable",
                JavaItemDataComponents.CONSUMABLE,
                (value, version) -> readConsumable(value.getAsJsonObject(), version)
        );
        register(
                "minecraft:food",
                JavaItemDataComponents.FOOD,
                (value, version) -> readFood(value.getAsJsonObject())
        );
        register(
                "minecraft:equippable",
                JavaItemDataComponents.EQUIPPABLE,
                (value, version) -> readEquippable(value.getAsJsonObject())
        );
        register(
                "minecraft:use_cooldown",
                JavaItemDataComponents.USE_COOLDOWN,
                (value, version) -> readCooldown(value.getAsJsonObject())
        );
        register(
                "minecraft:tool",
                JavaItemDataComponents.TOOL,
                (value, version) -> readTool(value.getAsJsonObject())
        );
        register(
                "minecraft:repairable",
                JavaItemDataComponents.REPAIRABLE,
                (value, version) -> JavaRepairable.of(readHolders(value.getAsJsonObject().get("items")))
        );
        register(
                "minecraft:attack_range",
                JavaItemDataComponents.ATTACK_RANGE,
                (value, version) -> readAttackRange(value.getAsJsonObject())
        );
        register(
                "minecraft:kinetic_weapon",
                JavaItemDataComponents.KINETIC_WEAPON,
                (value, version) -> readKineticWeapon(value.getAsJsonObject())
        );
        register(
                "minecraft:piercing_weapon",
                JavaItemDataComponents.PIERCING_WEAPON,
                (value, version) -> JavaPiercingWeapon.instance()
        );
        register(
                "minecraft:attack_animation",
                JavaItemDataComponents.ATTACK_ANIMATION,
                (value, version) -> readAttackAnimation(value.getAsJsonObject())
        );
        register(
                "minecraft:swing_animation",
                JavaItemDataComponents.ATTACK_ANIMATION,
                (value, version) -> readAttackAnimation(value.getAsJsonObject())
        );
        register(
                "minecraft:use_effects",
                JavaItemDataComponents.USE_EFFECTS,
                (value, version) -> readUseEffects(value.getAsJsonObject())
        );
        register(
                "minecraft:compostable",
                JavaItemDataComponents.COMPOSTABLE,
                (value, version) -> Unit.INSTANCE
        );
        register(
                "minecraft:cooking_fuel",
                JavaItemDataComponents.COOKING_FUEL,
                (value, version) -> Unit.INSTANCE
        );
        registerUnsupported("minecraft:interact_animation"); // Geyser API 不支持交互动画组件
        registerUnsupported("minecraft:blocks_attacks"); // Geyser API 未提供格挡组件接口
        registerUnsupported("minecraft:instrument"); // Geyser API 未提供乐器组件接口
    }

    private GeyserItemComponentConverter() {
    }

    static void apply(CustomItemDefinition.Builder builder, String key, @Nullable JsonElement value, int sourceVersion) {
        Converter converter = CONVERTERS.get(key);
        if (converter != null) {
            converter.apply(builder, value, sourceVersion);
        }
    }

    private static <T> void register(String key, ItemDataComponent<T> component, ComponentReader<T> reader) {
        CONVERTERS.put(key, (builder, value, version) -> {
            if (value == null) {
                builder.removeComponent(component);
            } else {
                builder.component(component, reader.read(value, version));
            }
        });
    }

    private static void registerUnsupported(String key) {
        CONVERTERS.put(key, (builder, value, version) -> {
        });
    }

    private static JavaConsumable readConsumable(JsonObject object, int sourceVersion) {
        String animation = object.has("animation") ? object.get("animation").getAsString() : "eat";
        // 旧版 spear 表示三叉戟，不能直接映射为新版长矛动画。
        if (animation.equals("trident") || (animation.equals("spear") && sourceVersion < 12111)) {
            throw new IllegalArgumentException("The API consumable animation enum cannot express TRIDENT");
        }
        float seconds = object.has("consume_seconds") ? object.get("consume_seconds").getAsFloat() : 1.6F;
        return JavaConsumable.of(seconds, JavaConsumable.Animation.valueOf(animation.toUpperCase(Locale.ROOT)));
    }

    private static JavaFoodProperties readFood(JsonObject object) {
        return JavaFoodProperties.of(
                object.get("nutrition").getAsInt(),
                object.get("saturation").getAsFloat(),
                object.has("can_always_eat") && object.get("can_always_eat").getAsBoolean()
        );
    }

    private static JavaEquippable readEquippable(JsonObject object) {
        return JavaEquippable.of(JavaEquippable.EquipmentSlot.valueOf(object.get("slot").getAsString().toUpperCase(Locale.ROOT)));
    }

    private static JavaUseCooldown readCooldown(JsonObject object) {
        JavaUseCooldown.Builder builder = JavaUseCooldown.builder().seconds(object.get("seconds").getAsFloat());
        if (object.has("cooldown_group")) {
            builder.cooldownGroup(Identifier.of(object.get("cooldown_group").getAsString()));
        }
        return builder.build();
    }

    private static JavaTool readTool(JsonObject object) {
        return JavaTool.of(!object.has("can_destroy_blocks_in_creative") || object.get("can_destroy_blocks_in_creative").getAsBoolean());
    }

    private static JavaAttackRange readAttackRange(JsonObject object) {
        return JavaAttackRange.builder()
                .minReach(object.has("min_reach") ? object.get("min_reach").getAsFloat() : 0)
                .maxReach(object.has("max_reach") ? object.get("max_reach").getAsFloat() : 3)
                .minCreativeReach(object.has("min_creative_reach") ? object.get("min_creative_reach").getAsFloat() : 0)
                .maxCreativeReach(object.has("max_creative_reach") ? object.get("max_creative_reach").getAsFloat() : 5)
                .hitboxMargin(object.has("hitbox_margin") ? object.get("hitbox_margin").getAsFloat() : 0.3F)
                .build();
    }

    private static JavaKineticWeapon readKineticWeapon(JsonObject object) {
        JavaKineticWeapon.Builder builder = JavaKineticWeapon.builder()
                .delayTicks(object.has("delay_ticks") ? object.get("delay_ticks").getAsInt() : 0);
        if (object.has("dismount_conditions")) {
            JsonObject condition = object.getAsJsonObject("dismount_conditions");
            builder.dismountConditions(JavaKineticWeapon.condition(
                    condition.get("max_duration_ticks").getAsInt(),
                    condition.has("min_speed") ? condition.get("min_speed").getAsFloat() : 0,
                    condition.has("min_relative_speed") ? condition.get("min_relative_speed").getAsFloat() : 0));
        }
        return builder.build();
    }

    private static JavaSwingAnimation readAttackAnimation(JsonObject object) {
        // Geyser 的攻击动画组件只接收时长，动作类型由资源包处理。
        return JavaSwingAnimation.of(object.has("duration") ? object.get("duration").getAsInt() : 6);
    }

    private static JavaUseEffects readUseEffects(JsonObject object) {
        return JavaUseEffects.of(object.has("speed_multiplier") ? object.get("speed_multiplier").getAsFloat() : 0.2F);
    }

    private static Holders readHolders(JsonElement value) {
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

    @FunctionalInterface
    private interface Converter {
        void apply(CustomItemDefinition.Builder builder, @Nullable JsonElement value, int sourceVersion);
    }

    @FunctionalInterface
    private interface ComponentReader<T> {
        T read(JsonElement value, int sourceVersion);
    }
}
