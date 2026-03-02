package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.joml.Vector3f;

public final class ModelEngineBlockEntityElementConfig implements BlockEntityElementConfig<ModelEngineBlockEntityElement> {
    private final Vector3f position;
    private final float yaw;
    private final float pitch;
    private final String model;

    public ModelEngineBlockEntityElementConfig(String model, Vector3f position, float yaw, float pitch) {
        this.pitch = pitch;
        this.position = position;
        this.yaw = yaw;
        this.model = model;
    }

    public String model() {
        return model;
    }

    public float pitch() {
        return pitch;
    }

    public Vector3f position() {
        return position;
    }

    public float yaw() {
        return yaw;
    }

    @Override
    public ModelEngineBlockEntityElement create(World world, BlockPos pos) {
        return new ModelEngineBlockEntityElement(world, pos, this);
    }

    @Override
    public Class<ModelEngineBlockEntityElement> elementClass() {
        return ModelEngineBlockEntityElement.class;
    }

    public static class Factory implements BlockEntityElementConfigFactory<ModelEngineBlockEntityElement> {

        @Override
        public ModelEngineBlockEntityElementConfig create(ConfigSection section) {
            String model = section.getNonEmptyString("model");
            return new ModelEngineBlockEntityElementConfig(
                    model,
                    section.getVector3f(new Vector3f(0.5f), "position"),
                    section.getFloat(0f, "yaw"),
                    section.getFloat(0f, "pitch")
            );
        }
    }
}
