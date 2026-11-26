package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.core.util.GsonHelper;

import static net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitleLayers.bind;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

final class BedrockActionBar {
    private static final String COLLECTION = "ce_action_layers";

    private BedrockActionBar() {
    }

    static void add(JsonObject hud) {
        String marker = ContainerTitleCodec.ACTIONBAR_MARKER;
        JsonObject bar = new JsonObject();
        bar.addProperty("$ce_action_raw", "$actionbar_text");
        bar.addProperty("$ce_managed|default", false);
        JsonArray nativeSize = new JsonArray(2);
        nativeSize.add("100%c + 12px");
        nativeSize.add("100%c + 5px");
        bar.add("$ce_action_size|default", nativeSize);
        bar.addProperty("size", "$ce_action_size");
        JsonObject managed = new JsonObject();
        String prefix = marker + "N";
        String plain = "(('%." + prefix.length() + "s' * $ce_action_raw) = '" + prefix + "')";
        String reset = "(('%." + (prefix.length() + 3) + "s' * $ce_action_raw) = '§r" + prefix + "')";
        managed.addProperty("requires", "(" + plain + " or " + reset + ")");
        managed.addProperty("$ce_managed", true);
        managed.add("$ce_action_size", GsonHelper.createArray(0, 0));
        JsonArray variables = new JsonArray();
        variables.add(managed);
        bar.add("variables", variables);

        JsonObject layers = new JsonObject();
        layers.addProperty("type", "stack_panel");
        layers.addProperty("orientation", "horizontal");
        layers.addProperty("ignored", "(not $ce_managed)");
        layers.add("size", GsonHelper.createArray(0, 0));
        layers.addProperty("collection_name", COLLECTION);
        JsonObject factory = new JsonObject();
        factory.addProperty("name", COLLECTION + "_factory");
        factory.addProperty("control_name", "hud.ce_action_layer");
        factory.addProperty("max_children_size", ContainerTitleCodec.MAX_BYTES / ContainerTitleCodec.ENTRY_BYTES);
        layers.add("factory", factory);
        JsonArray bindings = new JsonArray();
        JsonObject trigger = new JsonObject();
        trigger.addProperty("binding_name", "#gamepad_helper_visible");
        bindings.add(trigger);
        bind(bindings, "($ce_action_raw + ('%.0s' * #gamepad_helper_visible))", "#ce_raw");
        bind(bindings, "(#ce_raw - '§r" + marker + "' - '" + marker + "')", "#ce_data");
        bind(bindings, "('%." + ContainerTitleCodec.COUNT_BYTES + "s' * #ce_data)", "#ce_count");
        bind(bindings, "(#ce_count - 'N' - ':' + 0)", "#collection_length");
        bind(bindings, "(#ce_data - #ce_count)", "#ce_payload");
        layers.add("bindings", bindings);

        JsonObject append = new JsonObject();
        append.addProperty("array_name", "controls");
        append.addProperty("operation", "insert_back");
        append.add("value", createObject(COLLECTION, layers));
        JsonArray modifications = new JsonArray();
        modifications.add(append);
        bar.add("modifications", modifications);
        hud.add("hud_actionbar_text", bar);
        JsonObject nativeLabel = new JsonObject();
        nativeLabel.addProperty("ignored", "$ce_managed");
        hud.add("hud_actionbar_text/actionbar_message", nativeLabel);

        JsonObject label = new JsonObject();
        label.addProperty("type", "label");
        label.addProperty("layer", 31);
        label.addProperty("color", "$tool_tip_text");
        label.addProperty("shadow", true);
        label.addProperty("localize", false);
        label.addProperty("enable_profanity_filter", true);
        label.addProperty("alpha", "@hud.anim_actionbar_text_alpha_out");
        BedrockTitleLayers.bindLayer(label, COLLECTION, COLLECTION);
        hud.add("ce_action_layer", BedrockTitleLayers.positionedLayer(label, "#ce_layer", 1, false));
    }
}
