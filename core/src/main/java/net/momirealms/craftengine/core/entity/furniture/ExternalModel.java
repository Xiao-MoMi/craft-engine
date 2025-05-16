package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.AbstractEntity;

public interface ExternalModel {

    String plugin();

    String id();

    void bindModel(AbstractEntity entity);
}
