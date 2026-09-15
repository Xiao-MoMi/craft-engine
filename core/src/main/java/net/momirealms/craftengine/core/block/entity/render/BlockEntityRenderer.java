package net.momirealms.craftengine.core.block.entity.render;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.element.ConstantBlockEntityElement;
import net.momirealms.craftengine.core.entity.culling.ViewRangeCullable;
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

    public boolean cull(Player player) {
        boolean retained = false;
        for (BlockEntityElement element : this.elements) {
            if (element instanceof ViewRangeCullable display) {
                display.setCulled(player, true);
                retained = true;
            } else {
                element.hide(player);
            }
        }
        return retained;
    }

    public void restore(Player player) {
        for (BlockEntityElement element : this.elements) {
            if (!(element instanceof ViewRangeCullable display)) {
                // 不支持视距剔除的元素此前已被移除，通过正常显示流程重新生成。
                element.show(player);
                continue;
            }

            if (element instanceof ConstantBlockEntityElement constant && constant.hasCondition()) {
                // 显示条件可能已变化，且该元素此前可能因条件不满足而从未生成。
                // 先清理可能保留的旧实体，再由 show 重新判断条件并决定是否生成。
                element.hide(player);
                element.show(player);
                continue;
            }

            // 无显示条件的展示实体仍保留在客户端，只需恢复视距，无需重新生成。
            display.setCulled(player, false);
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
