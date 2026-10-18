package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.SelectItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public record JavaCrossbowModel(BaseItemModel idle, BaseItemModel pulling, List<JavaBowModel.Stage> stages,
                               float scale, BaseItemModel arrow, BaseItemModel rocket) {

    public static JavaCrossbowModel read(ItemModel model) throws IOException {
        if (model instanceof BaseItemModel) {
            BaseItemModel base = BedrockItemRendering.baseModel(model);
            return new JavaCrossbowModel(base, base, List.of(), 1, base, base);
        }
        if (!(model instanceof SelectItemModel select) || !(select.property() instanceof ChargeTypeSelectProperty)) {
            throw new IOException("Unsupported crossbow root: expected model or charge_type select");
        }
        BaseItemModel arrow = null;
        BaseItemModel rocket = null;
        for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : select.whenMap().entrySet()) {
            BaseItemModel base = BedrockItemRendering.baseModel(entry.getValue());
            for (JsonElement value : entry.getKey().map(List::of, values -> values)) {
                switch (value.getAsString()) {
                    case "arrow" -> arrow = base;
                    case "rocket" -> rocket = base;
                    default -> throw new IOException("Unsupported crossbow charge_type: " + value);
                }
            }
        }
        ItemModel fallback = select.fallBack();
        BaseItemModel idle;
        BaseItemModel pulling;
        List<JavaBowModel.Stage> stages = List.of();
        float scale = 1;
        if (fallback instanceof BaseItemModel) {
            idle = BedrockItemRendering.baseModel(fallback);
            pulling = idle;
        } else {
            if (!(fallback instanceof ConditionItemModel condition) || !(condition.property() instanceof UsingItemConditionProperty)) {
                throw new IOException("Unsupported crossbow fallback: expected model or using_item condition");
            }
            idle = BedrockItemRendering.baseModel(condition.onFalse());
            ItemModel using = condition.onTrue();
            if (using instanceof BaseItemModel) {
                pulling = BedrockItemRendering.baseModel(using);
            } else {
                if (!(using instanceof RangeDispatchItemModel range) || !(range.property() instanceof CrossBowPullingRangeDispatchProperty)) {
                    throw new IOException("Unsupported crossbow pulling model: expected crossbow/pull range_dispatch");
                }
                pulling = BedrockItemRendering.baseModel(range.fallBack());
                scale = range.scale();
                if (!Float.isFinite(scale)) {
                    throw new IOException("Invalid crossbow pull scale");
                }
                stages = JavaBowModel.stages(range);
            }
        }
        return new JavaCrossbowModel(idle, pulling, stages, scale, arrow == null ? idle : arrow, rocket == null ? idle : rocket);
    }
}
