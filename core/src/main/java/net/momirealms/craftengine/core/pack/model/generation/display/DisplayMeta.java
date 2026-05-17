package net.momirealms.craftengine.core.pack.model.generation.display;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import org.joml.Vector3f;

public record DisplayMeta(Vector3f rotation, Vector3f translation, Vector3f scale) {

    public static DisplayMeta fromConfig(ConfigSection section) {
        Vector3f rotation = section.getVector3f("rotation");
        Vector3f translation = section.getVector3f("translation");
        Vector3f scale = section.getVector3f("scale");
        return new DisplayMeta(rotation, translation, scale);
    }
}
