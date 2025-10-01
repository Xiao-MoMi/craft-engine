package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.joml.Vector3f;

import java.util.Map;

public class ModelEngineBlockEntityElementConfig implements BlockEntityElementConfig<ModelEngineBlockEntityElement> {
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

    public static class Factory implements BlockEntityElementConfigFactory {

        @SuppressWarnings("unchecked")
        @Override
        public <E extends BlockEntityElement> BlockEntityElementConfig<E> create(Map<String, Object> arguments) {
            String model = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("model"), "warning.config.block.state.entity_renderer.model_engine.missing_model");
            return (BlockEntityElementConfig<E>) new ModelEngineBlockEntityElementConfig(
                    model,
                    ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", 0.5f), "position"),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("yaw", 0f), "yaw"),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("pitch", 0f), "pitch")
            );
        }
    }
}
