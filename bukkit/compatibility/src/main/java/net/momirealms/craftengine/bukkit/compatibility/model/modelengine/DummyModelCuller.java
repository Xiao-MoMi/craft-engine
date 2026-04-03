package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.entity.CullType;
import com.ticxo.modelengine.api.entity.cull.ModelCuller;
import com.ticxo.modelengine.api.entity.data.IEntityData;

import java.util.UUID;

record DummyModelCuller(IEntityData data) implements ModelCuller {

    @Override
    public void setData(IEntityData data) {
    }

    @Override
    public IEntityData getData() {
        return this.data;
    }

    @Override
    public void updateCulledPlayer() {
    }

    @Override
    public CullType put(UUID uuid, CullType type) {
        return type;
    }

    @Override
    public CullType remove(UUID uuid) {
        return null;
    }

    @Override
    public int getCulledCount() {
        return 0;
    }
}
