package net.momirealms.craftengine.core.block.entity.render;

import net.momirealms.craftengine.core.block.entity.render.element.ConstantBlockEntityElement;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import org.jetbrains.annotations.Nullable;

public final class ConstantBlockEntityRenderer extends BlockEntityRenderer implements Cullable {
    public final CullingData cullingData;
    public final ConstantBlockEntityElement[] elements;

    public ConstantBlockEntityRenderer(ConstantBlockEntityElement[] constantElements, @Nullable CullingData cullingData) {
        super(constantElements);
        this.cullingData = cullingData;
        this.elements = constantElements;
    }

    @Override
    public ConstantBlockEntityElement[] elements() {
        return this.elements;
    }

    @Override
    public CullingData cullingData() {
        return this.cullingData;
    }
}
