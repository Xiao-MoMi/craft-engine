package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserItemComponentPatches {
    private static final Map<String, GeyserItemComponentReader> CONVERTERS = new LinkedHashMap<>();

    static {
        register("minecraft:max_stack_size", (value, version, itemType) -> GeyserItemComponents.stackSize(value));
        register("minecraft:max_damage", (value, version, itemType) -> GeyserItemComponents.durability(value));
        register("minecraft:enchantment_glint_override", (value, version, itemType) -> GeyserItemComponents.glint(value));
        register("minecraft:enchantable", (value, version, itemType) -> GeyserItemComponents.enchantable(value));
        register("minecraft:equippable", (value, version, itemType) -> GeyserItemComponents.equippable(value));
        register("minecraft:use_cooldown", (value, version, itemType) -> GeyserItemComponents.cooldown(value, itemType));
        register("minecraft:tool", (value, version, itemType) -> GeyserItemComponents.tool(value));
        register("minecraft:repairable", (value, version, itemType) -> GeyserItemComponents.repairable(value));
        register("minecraft:attack_animation", (value, version, itemType) -> GeyserItemComponents.swing(value));
        register("minecraft:swing_animation", (value, version, itemType) -> GeyserItemComponents.swing(value));
        register("minecraft:compostable", (value, version, itemType) -> GeyserItemComponents.compostable(value));
        register("minecraft:cooking_fuel", (value, version, itemType) -> GeyserItemComponents.fuel(value));
        register("minecraft:kinetic_weapon", (value, version, itemType) -> GeyserKineticWeaponComponentPatch.read(value));
        register("minecraft:piercing_weapon", (value, version, itemType) -> GeyserWeaponComponents.piercing(value));
        // 距离依赖武器组件，食物和使用效果依赖 consumable；固定顺序，不依赖 JSON 字段顺序。
        register("minecraft:attack_range", (value, version, itemType) -> GeyserWeaponComponents.range(value));
        register("minecraft:consumable", (value, version, itemType) -> GeyserConsumableComponents.consumable(value, version));
        register("minecraft:food", (value, version, itemType) -> GeyserConsumableComponents.food(value));
        register("minecraft:use_effects", (value, version, itemType) -> GeyserConsumableComponents.useEffects(value));
    }

    private GeyserItemComponentPatches() {
    }

    public static void register(String key, GeyserItemComponentReader converter) {
        CONVERTERS.put(key, converter);
    }

    @Nullable
    public static GeyserItemComponentPatch read(JsonObject components, int sourceVersion, String itemType) {
        GeyserItemComponentPatch patch = null;
        for (Map.Entry<String, GeyserItemComponentReader> converter : CONVERTERS.entrySet()) {
            JsonElement value = components.get(converter.getKey());
            if (value != null) {
                // 缺失继承基础物品，JSON null 移除；不再通过 Geyser 的 Java 组件 API 转换。
                patch = combine(patch, converter.getValue().read(value, sourceVersion, itemType));
            }
        }
        return patch;
    }

    @Nullable
    public static GeyserItemComponentPatch combine(@Nullable GeyserItemComponentPatch first, @Nullable GeyserItemComponentPatch second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return (data, mappings) -> second.apply(first.apply(data, mappings), mappings);
    }

    @Nullable
    public static GeyserItemComponentPatch cached(@Nullable GeyserItemComponentPatch patch) {
        if (patch == null) {
            return null;
        }
        // ItemMappings 属于协议注册表；缓存随本次计划持有，不保存玩家或会话。
        Map<ItemMappings, Map<NbtMap, NbtMap>> cache = new ConcurrentHashMap<>();
        return (data, mappings) -> cache.computeIfAbsent(mappings, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(data, original -> patch.apply(original, mappings));
    }
}
