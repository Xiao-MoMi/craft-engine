package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.RodCastConditionProperty;

import java.io.IOException;

public record JavaFishingRodModel(BaseItemModel idle, BaseItemModel cast) {

    public static JavaFishingRodModel read(ItemModel model) throws IOException {
        if (model instanceof BaseItemModel) {
            BaseItemModel base = BedrockItemRendering.baseModel(model);
            return new JavaFishingRodModel(base, base);
        }
        if (!(model instanceof ConditionItemModel condition) || !(condition.property() instanceof RodCastConditionProperty)) {
            throw new IOException("Unsupported fishing rod root: expected model or fishing_rod/cast condition");
        }
        return new JavaFishingRodModel(BedrockItemRendering.baseModel(condition.onFalse()),
                BedrockItemRendering.baseModel(condition.onTrue()));
    }
}
