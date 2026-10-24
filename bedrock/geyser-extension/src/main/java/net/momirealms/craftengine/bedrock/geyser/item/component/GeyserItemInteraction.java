package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.Nullable;

final class GeyserItemInteraction {
    private static final NbtMap USE_BUTTON = NbtMap.builder().putString("value", "action.interact.use").build();

    private GeyserItemInteraction() {
    }

    static GeyserItemComponentPatch read(@Nullable JsonObject behavior, JsonObject components) {
        boolean using = false;
        if (behavior != null) {
            JsonObject use = behavior.getAsJsonObject("use");
            using = !use.get("animation").getAsString().equals("NONE")
                    || use.getAsJsonObject("maximumDuration").get("value").getAsInt() > 0
                    || use.get("nativeUseOnRelease").getAsBoolean();
        }
        JsonElement equippable = components.get("minecraft:equippable");
        boolean swappable = true;
        if (equippable != null && !equippable.isJsonNull()) {
            JsonObject equipment = equippable.getAsJsonObject();
            swappable = !equipment.has("swappable") || equipment.get("swappable").getAsBoolean();
        }
        boolean useAction = using;
        boolean equipAction = swappable;
        return GeyserItemComponents.update((converted, properties) -> {
            NbtMap wearable = (NbtMap) converted.getOrDefault("minecraft:wearable", NbtMap.EMPTY);
            boolean playerEquipment = switch (wearable.getString("slot")) {
                case "slot.armor.head", "slot.armor.chest", "slot.armor.legs", "slot.armor.feet", "slot.weapon.offhand" -> true;
                default -> false;
            };
            // 展示方块的 block_placer 仅用于渲染，不代表物品真的有使用动作。
            boolean show = useAction || (equipAction && playerEquipment)
                    || converted.containsKey("minecraft:food") || converted.containsKey("minecraft:shooter")
                    || converted.containsKey("minecraft:throwable") || converted.containsKey("minecraft:entity_placer");
            GeyserItemComponents.set(converted, "minecraft:interact_button", show ? USE_BUTTON : null);
        });
    }
}
