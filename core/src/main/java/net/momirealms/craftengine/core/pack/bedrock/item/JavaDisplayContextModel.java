package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.SelectItemModel;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record JavaDisplayContextModel(BaseItemModel fallback, Map<DisplayPosition, BaseItemModel> contexts) {

    public JavaDisplayContextModel {
        contexts = Map.copyOf(contexts);
    }

    public BaseItemModel model(DisplayPosition position) {
        return this.contexts.getOrDefault(position, this.fallback);
    }

    public static JavaDisplayContextModel read(ItemModel model) throws IOException {
        if (model instanceof BaseItemModel) {
            return new JavaDisplayContextModel(BedrockItemRendering.baseModel(model), Map.of());
        }
        if (!(model instanceof SelectItemModel select) || !(select.property() instanceof DisplayContextSelectProperty)) {
            throw new IOException("Unsupported spear root: expected model or display_context select");
        }
        Map<DisplayPosition, BaseItemModel> contexts = new EnumMap<>(DisplayPosition.class);
        for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : select.whenMap().entrySet()) {
            BaseItemModel base = BedrockItemRendering.baseModel(entry.getValue());
            for (JsonElement element : entry.getKey().map(List::of, values -> values)) {
                String value = element.getAsString();
                if (value.equals("on_shelf") || value.equals("none")) {
                    continue;
                }
                DisplayPosition position = DisplayPosition.valueOf(value.toUpperCase(Locale.ROOT));
                contexts.putIfAbsent(position, base);
            }
        }
        return new JavaDisplayContextModel(BedrockItemRendering.baseModel(select.fallBack()), contexts);
    }
}
