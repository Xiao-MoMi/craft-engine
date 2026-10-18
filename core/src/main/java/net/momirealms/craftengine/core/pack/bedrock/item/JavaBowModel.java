package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.UseDurationRangeDispatchProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record JavaBowModel(BaseItemModel idle, BaseItemModel pulling, List<Stage> stages, float scale, boolean remaining) {

    public static JavaBowModel read(ItemModel model) throws IOException {
        if (model instanceof BaseItemModel) {
            BaseItemModel base = BedrockItemRendering.baseModel(model);
            return new JavaBowModel(base, base, List.of(), 1, false);
        }
        if (!(model instanceof ConditionItemModel condition) || !(condition.property() instanceof UsingItemConditionProperty)) {
            throw new IOException("Unsupported bow root: expected model or using_item condition");
        }
        BaseItemModel idle = BedrockItemRendering.baseModel(condition.onFalse());
        ItemModel using = condition.onTrue();
        if (using instanceof BaseItemModel) {
            return new JavaBowModel(idle, BedrockItemRendering.baseModel(using), List.of(), 1, false);
        }
        if (!(using instanceof RangeDispatchItemModel range) || !(range.property() instanceof UseDurationRangeDispatchProperty duration)) {
            throw new IOException("Unsupported pulling model: expected model or use_duration range_dispatch");
        }
        if (range.fallBack() == null) {
            throw new IOException("Bow use_duration requires a fallback model");
        }
        if (!Float.isFinite(range.scale())) {
            throw new IOException("Invalid bow use_duration scale");
        }
        return new JavaBowModel(idle, BedrockItemRendering.baseModel(range.fallBack()), stages(range), range.scale(), duration.remaining());
    }

    static List<Stage> stages(RangeDispatchItemModel range) throws IOException {
        List<Stage> stages = new ArrayList<>(range.entries().size());
        for (Map.Entry<Float, ItemModel> entry : range.entries().entrySet()) {
            float threshold = entry.getKey();
            if (!Float.isFinite(threshold)) {
                throw new IOException("Invalid item model range_dispatch threshold");
            }
            stages.add(new Stage(threshold, BedrockItemRendering.baseModel(entry.getValue())));
        }
        stages.sort(Comparator.comparingDouble(Stage::threshold));
        return List.copyOf(stages);
    }

    public record Stage(float threshold, BaseItemModel model) {
    }
}
