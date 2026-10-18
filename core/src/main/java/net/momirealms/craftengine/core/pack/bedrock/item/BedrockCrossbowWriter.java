package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockCrossbowWriter {
    private final BedrockStateItemWriter writer;
    private final BedrockNativeItemWriter nativeItems;

    public BedrockCrossbowWriter(JavaModelResolver resolver, BedrockTextureAtlas.TextureSource textures, BedrockItemIcons icons,
                                 BedrockBlockItems blocks, Path root) {
        this.writer = new BedrockStateItemWriter(resolver, textures, icons, blocks, root);
        this.nativeItems = new BedrockNativeItemWriter(resolver, textures, icons, blocks, root);
    }

    public Result write(JavaCrossbowModel crossbow, List<BedrockItemSource> items) throws IOException {
        Key owner = items.getFirst().id();
        Map<JavaItemModelLeaf, Integer> states = new LinkedHashMap<>();
        states.put(crossbow.idle(), 0);
        states.computeIfAbsent(crossbow.pulling(), ignored -> states.size());
        for (JavaBowModel.Stage stage : crossbow.stages()) {
            states.computeIfAbsent(stage.model(), ignored -> states.size());
        }
        states.computeIfAbsent(crossbow.arrow(), ignored -> states.size());
        states.computeIfAbsent(crossbow.rocket(), ignored -> states.size());
        JsonArray update = new JsonArray();
        update.add("variable.ce_crossbow_charge = 0;");
        update.add("variable.ce_crossbow_using = context.item_slot == 'main_hand' && variable.ce_crossbow_charge == 0 && query.main_hand_item_use_duration > 0;");
        // The normalized native draw query includes Quick Charge; a fixed Java tick count would finish at the wrong time.
        update.add("variable.ce_crossbow_pull = math.clamp(1 - query.item_remaining_use_duration('main_hand', 1.0), 0, 1);");
        update.add("variable.ce_crossbow_progress = variable.ce_crossbow_pull * " + crossbow.scale() + ';');
        String selected = Integer.toString(states.get(crossbow.pulling()));
        for (JavaBowModel.Stage stage : crossbow.stages()) {
            selected = "(variable.ce_crossbow_progress >= " + stage.threshold() + " ? " + states.get(stage.model()) + " : " + selected + ')';
        }
        update.add("variable.ce_crossbow_state = variable.ce_crossbow_charge == 1 ? " + states.get(crossbow.arrow())
                + " : (variable.ce_crossbow_charge == 2 ? " + states.get(crossbow.rocket())
                + " : (variable.ce_crossbow_using ? " + selected + " : 0));");
        Map<JavaItemModelLeaf, String> conditions = new LinkedHashMap<>();
        for (Map.Entry<JavaItemModelLeaf, Integer> state : states.entrySet()) {
            conditions.put(state.getKey(), "variable.ce_crossbow_state == " + state.getValue());
        }
        BedrockStateItemWriter.Result result = this.writer.write(owner, "xbow", conditions, crossbow.idle(), update, false, BedrockWeaponDisplay.CROSSBOW);
        BedrockNativeItemWriter.Appearance arrow = this.nativeItems.write(crossbow.arrow(), chargedId(owner, false));
        BedrockNativeItemWriter.Appearance rocket = this.nativeItems.write(crossbow.rocket(), chargedId(owner, true));
        JsonObject attachable = result.attachable();
        for (BedrockItemSource item : items) {
            update.set(0, new JsonPrimitive("variable.ce_crossbow_charge = 0;"));
            this.writer.writeAttachable(attachable, item.id());
            update.set(0, new JsonPrimitive("variable.ce_crossbow_charge = 1;"));
            this.writer.writeAttachable(attachable, chargedId(item.id(), false));
            update.set(0, new JsonPrimitive("variable.ce_crossbow_charge = 2;"));
            this.writer.writeAttachable(attachable, chargedId(item.id(), true));
        }
        return new Result(result.icon(), result.displayBlock(), arrow, rocket, result.models());
    }

    public static Key chargedId(Key item, boolean rocket) {
        return Key.of("ce", (rocket ? "xb_r_" : "xb_a_") + HashUtils.sha1(item.asString().getBytes(StandardCharsets.UTF_8)));
    }

    public record Result(String icon, BedrockPackMappings.DisplayBlock displayBlock, BedrockNativeItemWriter.Appearance arrow,
                         BedrockNativeItemWriter.Appearance rocket, int models) {
    }
}
