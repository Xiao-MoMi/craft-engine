package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockBowWriter {
    private final BedrockStateItemWriter writer;

    public BedrockBowWriter(JavaModelResolver resolver, BedrockTextureAtlas.TextureSource textures, BedrockItemIcons icons,
                            BedrockBlockItems blockItems, Path root) {
        this.writer = new BedrockStateItemWriter(resolver, textures, icons, blockItems, root);
    }

    public Result write(JavaBowModel bow, List<BedrockItemSource> items) throws IOException {
        Key owner = items.getFirst().id();
        Map<BaseItemModel, Integer> states = new LinkedHashMap<>();
        states.put(bow.idle(), 0);
        states.computeIfAbsent(bow.pulling(), ignored -> states.size());
        for (JavaBowModel.Stage stage : bow.stages()) {
            states.computeIfAbsent(stage.model(), ignored -> states.size());
        }
        JsonArray update = new JsonArray();
        // Match vanilla bow.json's tick clock, while retaining the Java pack's scale and thresholds.
        update.add("variable.ce_bow_using = context.item_slot == 'main_hand' && query.main_hand_item_use_duration > 0.0;");
        update.add("variable.ce_bow_ticks = math.max(0, query.main_hand_item_max_duration - (query.main_hand_item_use_duration - query.frame_alpha + 1.0));");
        String time = bow.remaining() ? "math.max(0, query.main_hand_item_use_duration - query.frame_alpha + 1.0)" : "variable.ce_bow_ticks";
        update.add("variable.ce_bow_duration = (" + time + ") * " + bow.scale() + ";");
        String selected = Integer.toString(states.get(bow.pulling()));
        for (JavaBowModel.Stage stage : bow.stages()) {
            selected = "(variable.ce_bow_duration >= " + stage.threshold() + " ? " + states.get(stage.model()) + " : " + selected + ')';
        }
        update.add("variable.ce_bow_state = variable.ce_bow_using ? " + selected + " : 0;");
        update.add("variable.ce_bow_late_stage = " + (bow.stages().isEmpty() ? "0" : "variable.ce_bow_duration >= " + bow.stages().getFirst().threshold()) + ';');
        Map<BaseItemModel, String> conditions = new LinkedHashMap<>();
        for (Map.Entry<BaseItemModel, Integer> state : states.entrySet()) {
            conditions.put(state.getKey(), "variable.ce_bow_state == " + state.getValue());
        }
        BedrockStateItemWriter.Result result = this.writer.write(owner, "bow", conditions, bow.idle(), update, true, null);
        for (BedrockItemSource item : items) {
            this.writer.writeAttachable(result.attachable(), item.id());
        }
        return new Result(result.icon(), result.displayBlock(), result.models());
    }

    public record Result(String icon, BedrockPackMappings.DisplayBlock displayBlock, int models) {
    }
}
