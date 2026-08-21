package net.momirealms.craftengine.core.block.entity.render;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import net.momirealms.craftengine.core.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class DynamicBlockEntityRenderer extends BlockEntityRenderer implements Cullable {
    private volatile @Nullable CullingData cullingData;

    public DynamicBlockEntityRenderer(BlockEntityElement[] elements) {
        super(elements);
    }

    @Nullable
    @Override
    public CullingData cullingData() {
        return this.cullingData;
    }

    public void setCullingData(@Nullable CullingData cullingData) {
        this.cullingData = cullingData;
    }

    public boolean initialForceVisible(Player player) {
        for (BlockEntityElement element : this.elements) {
            if (element.initialForceVisible(player)) {
                return true;
            }
        }
        return false;
    }
}
