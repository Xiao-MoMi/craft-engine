package net.momirealms.craftengine.bukkit.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationProperties;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationReader;
import net.momirealms.craftengine.core.util.Key;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BukkitItemRegistrationReader implements ItemRegistrationReader {
    private static final Set<Key> EFFECTIVE_COMPONENTS = Set.of(DataComponentKeys.EQUIPPABLE, DataComponentKeys.KINETIC_WEAPON);
    private static final List<Key> COMPONENTS = List.of(
            DataComponentKeys.CONSUMABLE,
            DataComponentKeys.FOOD,
            DataComponentKeys.EQUIPPABLE,
            DataComponentKeys.MAX_DAMAGE,
            DataComponentKeys.MAX_STACK_SIZE,
            DataComponentKeys.USE_COOLDOWN,
            DataComponentKeys.ENCHANTABLE,
            DataComponentKeys.TOOL,
            DataComponentKeys.REPAIRABLE,
            DataComponentKeys.ENCHANTMENT_GLINT_OVERRIDE,
            DataComponentKeys.ATTACK_RANGE,
            DataComponentKeys.KINETIC_WEAPON,
            DataComponentKeys.PIERCING_WEAPON,
            DataComponentKeys.USE_EFFECTS,
            DataComponentKeys.SWING_ANIMATION,
            DataComponentKeys.ATTACK_ANIMATION,
            DataComponentKeys.INTERACT_ANIMATION,
            DataComponentKeys.BLOCKS_ATTACK,
            DataComponentKeys.INSTRUMENT,
            DataComponentKeys.COMPOSTABLE,
            DataComponentKeys.COOKING_FUEL);
    private final Map<Key, Object> componentTypes;

    public BukkitItemRegistrationReader() {
        Map<Key, Object> componentTypes = new LinkedHashMap<>();
        for (Key key : COMPONENTS) {
            Object type = DataComponentTypes.byId(key);
            if (type != null) {
                componentTypes.put(key, type);
            }
        }
        this.componentTypes = Map.copyOf(componentTypes);
    }

    @Override
    public ItemRegistrationProperties read(Item item, ItemBehaviorDescription behavior) {
        ItemBehaviorDescription.Use use = behavior.use();
        boolean hasUseBehavior = use.animation() != ItemBehaviorDescription.UseAnimation.NONE || use.maximumDuration().value() > 0 || use.nativeUseOnRelease();
        Map<Key, JsonElement> components = new LinkedHashMap<>();
        for (Map.Entry<Key, Object> entry : this.componentTypes.entrySet()) {
            JsonElement effective = item.getComponentAsJson(entry.getValue());
            JsonElement defaultValue = item.type().getJsonComponent(entry.getValue()).orElse(null);
            // 默认使用效果只对有使用行为的物品收集；组件覆写和显式移除始终保留。
            if (Objects.equals(effective, defaultValue)) {
                boolean collectDefault = EFFECTIVE_COMPONENTS.contains(entry.getKey()) || (hasUseBehavior && entry.getKey().equals(DataComponentKeys.USE_EFFECTS));
                if (effective == null || !collectDefault) {
                    continue;
                }
            }
            components.put(entry.getKey(), effective == null ? JsonNull.INSTANCE : effective);
        }
        return new ItemRegistrationProperties(components);
    }
}
