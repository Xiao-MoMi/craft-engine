package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.entity.furniture.AbstractExternalModel;

public class ModelEngineModel extends AbstractExternalModel {

    public ModelEngineModel(String id) {
        super(id);
    }

    @Override
    public String plugin() {
        return "ModelEngine";
    }

    @Override
    public void bindModel(AbstractEntity entity) {
        org.bukkit.entity.Entity bukkitEntity = (org.bukkit.entity.Entity) entity.literalObject();
        ModelEngineUtils.bindModel(bukkitEntity, id());
    }
}
