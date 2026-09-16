package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

public record FurnitureVariant(@NotNull String name,
                               @Nullable CullingData cullingData,
                               @NotNull List<FurnitureElementConfig<? extends FurnitureElement>> elementConfigs,
                               @NotNull List<FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitBoxConfigs,
                               @Nullable Supplier<ExternalModel> externalModel,
                               @NotNull Vector3f dropOffset,
                               @NotNull ColliderMergePlan colliderMergePlan) {

    public FurnitureVariant(String name, CullingData cullingData,
                            List<FurnitureElementConfig<? extends FurnitureElement>> elementConfigs,
                            List<FurnitureHitBoxConfig<? extends FurnitureHitBox>> hitBoxConfigs,
                            Supplier<ExternalModel> externalModel, Vector3f dropOffset) {
        this(name, cullingData, List.copyOf(elementConfigs), List.copyOf(hitBoxConfigs), externalModel, dropOffset, ColliderMergePlan.compile(hitBoxConfigs));
    }
}