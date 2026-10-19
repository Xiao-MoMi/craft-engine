package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.definition.analysis.ItemModelNode;
import net.momirealms.craftengine.core.pack.model.definition.analysis.ItemModelProgram;

import java.io.IOException;

public final class BedrockItemModelSupport {

    private BedrockItemModelSupport() {
    }

    public static BaseItemModel requireStaticModel(ItemModelProgram program) throws IOException {
        ModernItemModel definition = program.definition();
        if (definition.oversizedInGui() || !definition.handAnimationOnSwap() || definition.swapAnimationScale() != 1) {
            throw new IOException("Bedrock backend does not support Java item display settings at $");
        }
        ItemModelNode root = program.root();
        if (!(root instanceof ItemModelNode.Model model)) {
            throw new IOException("Bedrock backend does not support " + root.getClass().getSimpleName() + " at " + root.path());
        }
        if (model.transformation() != null) {
            throw new IOException("Bedrock backend does not support item model transformation at " + root.path());
        }
        return model.definition();
    }
}
