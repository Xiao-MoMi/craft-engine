package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import org.joml.Vector3f;

import java.util.Map;

public final class BetterModelBlockEntityElementConfig implements BlockEntityElementConfig<BetterModelBlockEntityElement> {
    private final Vector3f position;
    private final float yaw;
    private final float pitch;
    private final String model;
    private final boolean sightTrace;

    public BetterModelBlockEntityElementConfig(String model,
                                               Vector3f position,
                                               float yaw,
                                               float pitch,
                                               boolean sightTrace) {
        this.pitch = pitch;
        this.position = position;
        this.yaw = yaw;
        this.model = model;
        this.sightTrace = sightTrace;
    }

    public String model() {
        return this.model;
    }

    public float pitch() {
        return this.pitch;
    }

    public Vector3f position() {
        return this.position;
    }

    public float yaw() {
        return this.yaw;
    }

    public boolean sightTrace() {
        return this.sightTrace;
    }

    @Override
    public BetterModelBlockEntityElement create(World world, BlockPos pos) {
        return new BetterModelBlockEntityElement(world, pos, this);
    }

    @Override
    public Class<BetterModelBlockEntityElement> elementClass() {
        return BetterModelBlockEntityElement.class;
    }

    public static class Factory implements BlockEntityElementConfigFactory<BetterModelBlockEntityElement> {

        @Override
        public BetterModelBlockEntityElementConfig create(Map<String, Object> arguments) {
            String model = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("model"), "warning.config.block.state.entity_renderer.better_model.missing_model");
            return new BetterModelBlockEntityElementConfig(
                    model,
                    ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", 0.5f), "position"),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("yaw", 0f), "yaw"),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("pitch", 0f), "pitch"),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("sight-trace", true), "sight-trace")
            );
        }
    }
}
