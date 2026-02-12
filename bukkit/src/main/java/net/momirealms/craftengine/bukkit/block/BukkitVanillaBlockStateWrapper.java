package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockRegistryMirror;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.StatePropertyAccessor;

import java.util.Collection;

public class BukkitVanillaBlockStateWrapper extends BukkitBlockStateWrapper {
    private final StatePropertyAccessor accessor;

    public BukkitVanillaBlockStateWrapper(Object blockState, int registryId) {
        super(blockState, registryId);
        this.accessor = FastNMS.INSTANCE.createStatePropertyAccessor(blockState);
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    @Override
    public <T> T getProperty(String propertyName) {
        return this.accessor.getPropertyValue(propertyName);
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return this.accessor.hasProperty(propertyName);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return this.accessor.getPropertyNames();
    }


    @Override
    public BlockStateWrapper withProperty(String propertyName, String propertyValue) {
        Object newState = this.accessor.withProperty(propertyName, propertyValue);
        if (newState == super.blockState) return this;
        return BlockRegistryMirror.byId(BlockStateUtils.blockStateToId(newState));
    }

    @Override
    public BlockStateWrapper cycleProperty(String propertyName, boolean backwards) {
        Object newState = this.accessor.cycleProperty(propertyName, backwards);
        if (newState == super.blockState) return this;
        return BlockRegistryMirror.byId(BlockStateUtils.blockStateToId(newState));
    }
}
