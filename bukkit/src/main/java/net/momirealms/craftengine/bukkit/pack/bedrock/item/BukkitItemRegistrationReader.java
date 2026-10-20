package net.momirealms.craftengine.bukkit.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemBehaviorDescription.Component;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationProperties;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationReader;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BukkitItemRegistrationReader implements ItemRegistrationReader {
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
            DataComponentKeys.ENCHANTMENT_GLINT_OVERRIDE, Key.minecraft("attack_range"),
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
    public ItemRegistrationProperties read(Item item) {
        Map<Key, Component> components = new LinkedHashMap<>();
        for (Map.Entry<Key, Object> entry : this.componentTypes.entrySet()) {
            JsonElement effective = item.getComponentAsJson(entry.getValue());
            JsonElement defaultValue = item.type().getJsonComponent(entry.getValue()).orElse(null);
            components.put(entry.getKey(), Component.of(effective, defaultValue));
        }
        return new ItemRegistrationProperties(components);
    }
}
