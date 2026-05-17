package net.momirealms.craftengine.core.block.entity.render;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.entity.player.Player;

public class BlockEntityRenderer {
    protected final BlockEntityElement[] elements;

    public BlockEntityRenderer(BlockEntityElement[] elements) {
        this.elements = elements;
    }

    public BlockEntityElement[] elements() {
        return this.elements;
    }

    public void show(Player player) {
        for (BlockEntityElement element : this.elements) {
            element.show(player);
        }
    }

    public void hide(Player player) {
        for (BlockEntityElement element : this.elements) {
            element.hide(player);
        }
    }

    public void update(Player player) {
        for (BlockEntityElement element : this.elements) {
            element.update(player);
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
}
