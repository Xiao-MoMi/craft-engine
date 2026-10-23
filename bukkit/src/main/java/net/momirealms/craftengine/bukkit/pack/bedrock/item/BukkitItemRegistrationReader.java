package net.momirealms.craftengine.bukkit.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationProperties;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationReader;
import net.momirealms.craftengine.core.util.Key;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public ItemRegistrationProperties read(Item item) {
        Map<Key, JsonElement> components = new LinkedHashMap<>();
        JsonObject kineticWeapon = null;
        for (Map.Entry<Key, Object> entry : this.componentTypes.entrySet()) {
            JsonElement effective = item.getComponentAsJson(entry.getValue());
            if (entry.getKey().equals(DataComponentKeys.KINETIC_WEAPON) && effective != null) {
                kineticWeapon = effective.getAsJsonObject();
            }
            JsonElement defaultValue = item.type().getJsonComponent(entry.getValue()).orElse(null);
            if (Objects.equals(effective, defaultValue)) {
                continue;
            }
            components.put(entry.getKey(), effective == null ? JsonNull.INSTANCE : effective);
        }
        return new ItemRegistrationProperties(components, kineticWeapon);
    }
}
