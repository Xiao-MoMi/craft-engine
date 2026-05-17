package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public final class ModelEngineFurnitureElementConfig implements FurnitureElementConfig<ModelEngineFurnitureElement> {
    public static final FurnitureElementConfigFactory<ModelEngineFurnitureElement> FACTORY = new Factory();
    public final Vector3f position;
    public final float yaw;
    public final float pitch;
    public final String model;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ModelEngineFurnitureElementConfig(String model,
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

    @Override
    public ModelEngineFurnitureElement create(@NotNull Furniture furniture) {
        return new ModelEngineFurnitureElement(furniture, this);
    }

    private static class Factory implements FurnitureElementConfigFactory<ModelEngineFurnitureElement> {

        @Override
        public FurnitureElementConfig<ModelEngineFurnitureElement> create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            return new ModelEngineFurnitureElementConfig(
                    section.getNonEmptyString("model"),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("yaw"),
                    section.getFloat("pitch"),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
