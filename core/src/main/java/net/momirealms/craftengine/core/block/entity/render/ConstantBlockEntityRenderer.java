package net.momirealms.craftengine.core.block.entity.render;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import org.jetbrains.annotations.Nullable;

public final class ConstantBlockEntityRenderer implements Cullable {
    private final BlockEntityElement[] elements;
    public final CullingData cullingData;

    public ConstantBlockEntityRenderer(BlockEntityElement[] elements, @Nullable CullingData cullingData) {
        this.elements = elements;
        this.cullingData = cullingData;
    }

    @Override
    public void show(Player player) {
        for (BlockEntityElement element : this.elements) {
            if (element != null) {
                element.show(player);
            }
        }
    }

    @Override
    public void hide(Player player) {
        for (BlockEntityElement element : this.elements) {
            if (element != null) {
                element.hide(player);
            }
        }
    }

    public void deactivate() {
        for (BlockEntityElement element : this.elements) {
            if (element != null) {
                element.deactivate();
            }
        }
    }

    public void activate() {
        for (BlockEntityElement element : this.elements) {
            if (element != null) {
                element.activate();
            }
        }
    }

    public BlockEntityElement[] elements() {
        return this.elements;
    }

    @Override
    public CullingData cullingData() {
        return this.cullingData;
    }

    public boolean canCull() {
        return this.cullingData != null;
    }
}
