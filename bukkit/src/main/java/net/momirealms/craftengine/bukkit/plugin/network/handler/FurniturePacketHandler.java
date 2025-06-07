package net.momirealms.craftengine.bukkit.plugin.network.handler;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.plugin.network.EntityPacketHandler;

import java.util.List;

public class FurniturePacketHandler implements EntityPacketHandler {
    private final List<Integer> fakeEntities;

    public FurniturePacketHandler(List<Integer> fakeEntities) {
        this.fakeEntities = fakeEntities;
    }

    @Override
    public boolean handleEntitiesRemove(IntList entityIds) {
        entityIds.addAll(this.fakeEntities);
        return true;
    }
}
