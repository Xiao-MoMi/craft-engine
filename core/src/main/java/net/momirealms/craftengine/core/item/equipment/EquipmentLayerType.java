package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum EquipmentLayerType {
    WOLF_BODY("wolf_body"),
    HORSE_BODY("horse_body"),
    LLAMA_BODY("llama_body"),
    HUMANOID("humanoid"),
    HUMANOID_LEGGINGS("humanoid_leggings"),
    WINGS("wings"),
    PIG_SADDLE("pig_saddle"),
    STRIDER_SADDLE("strider_saddle"),
    CAMEL_SADDLE("camel_saddle"),
    CAMEL_HUSK_SADDLE("camel_husk_saddle"),
    HORSE_SADDLE("horse_saddle"),
    DONKEY_SADDLE("donkey_saddle"),
    MULE_SADDLE("mule_saddle"),
    NAUTILUS_BODY("nautilus_body"),
    SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
    ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
    HAPPY_GHAST_BODY("happy_ghast_body");

    private static final Map<String, EquipmentLayerType> BY_ID = new HashMap<>();

    static {
        for (EquipmentLayerType type : EquipmentLayerType.values()) {
            BY_ID.put(type.id(), type);
        }
    }

    private final String id;

    EquipmentLayerType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Nullable
    public static EquipmentLayerType byId(String id) {
        return BY_ID.get(StringUtils.normalizeSettingsType(id));
    }
}
