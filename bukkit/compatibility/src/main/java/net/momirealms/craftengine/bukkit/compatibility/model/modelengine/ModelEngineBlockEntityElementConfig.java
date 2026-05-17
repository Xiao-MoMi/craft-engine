package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public final class ModelEngineBlockEntityElementConfig implements BlockEntityElementConfig<ModelEngineBlockEntityElement> {
    public final Vector3f position;
    public final float yaw;
    public final float pitch;
    public final String model;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    public ModelEngineBlockEntityElementConfig(String model,
                                               Vector3f position,
                                               float yaw,
                                               float pitch,
                                               Predicate<PlayerContext> predicate,
                                               boolean hasCondition) {
        this.pitch = pitch;
        this.position = position;
        this.yaw = yaw;
        this.model = model;
        this.predicate = predicate;
        this.hasCondition = hasCondition;
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
    public ModelEngineBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new ModelEngineBlockEntityElement(this, chunk.world.world, pos);
    }

    @Override
    public Class<ModelEngineBlockEntityElement> elementClass() {
        return ModelEngineBlockEntityElement.class;
    }

    public static class Factory implements BlockEntityElementConfigFactory<ModelEngineBlockEntityElement> {

        @Override
        public ModelEngineBlockEntityElementConfig create(ConfigSection section) {
            String model = section.getNonEmptyString("model");
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            return new ModelEngineBlockEntityElementConfig(
                    model,
                    section.getVector3f("position", ConfigConstants.CENTER_VECTOR3),
                    section.getFloat("yaw"),
                    section.getFloat("pitch"),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
