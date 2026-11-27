package net.momirealms.craftengine.core.pack.model.generation.display;

import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import org.joml.Vector3f;

public record DisplayMeta(Vector3f rotation, Vector3f translation, Vector3f scale) {

    public static DisplayMeta fromConfig(ConfigSection section) {
        Vector3f rotation = section.getVector3f(ModelJsonKeys.ROTATION);
        Vector3f translation = section.getVector3f(ModelJsonKeys.TRANSLATION);
        Vector3f scale = section.getVector3f(ModelJsonKeys.SCALE);
        return new DisplayMeta(rotation, translation, scale);
    }
}
