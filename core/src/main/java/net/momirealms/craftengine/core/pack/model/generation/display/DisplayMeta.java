package net.momirealms.craftengine.core.pack.model.generation.display;

import net.momirealms.craftengine.core.util.MiscUtils;
import org.joml.Vector3f;

import java.util.Map;

public record DisplayMeta(Vector3f rotation, Vector3f translation, Vector3f scale) {

    public static DisplayMeta fromMap(Map<String, Object> map) {
        Vector3f rotation = null;
        if (map.containsKey("rotation")) {
            rotation = MiscUtils.getAsVector3f(map.get("rotation"), "rotation");
        }
        Vector3f translation = null;
        if (map.containsKey("translation")) {
            translation = MiscUtils.getAsVector3f(map.get("translation"), "translation");
        }
        Vector3f scale = null;
        if (map.containsKey("scale")) {
            scale = MiscUtils.getAsVector3f(map.get("scale"), "scale");
        }
        return new DisplayMeta(rotation, translation, scale);
    }
}
