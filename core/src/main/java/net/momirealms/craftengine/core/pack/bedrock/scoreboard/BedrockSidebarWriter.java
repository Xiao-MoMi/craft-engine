package net.momirealms.craftengine.core.pack.bedrock.scoreboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.SidebarCodec;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class BedrockSidebarWriter {
    private static final int HEADER_BYTES = SidebarCodec.PREFIX_BYTES + ContainerTitleCodec.COUNT_BYTES;
    private final JsonArray profiles;
    private final JsonObject controls = new JsonObject();

    public BedrockSidebarWriter(JsonArray profiles) {
        this.profiles = profiles;
        this.controls.addProperty("namespace", "scoreboard");
    }

    public void write(Path root) throws IOException {
        this.controls.add("ce_sidebar_title_layer", this.layer(true));
        this.controls.add("ce_sidebar_player_layer", this.layer(false));
        this.controls.add("scoreboard_sidebar_player", this.line(false));
        this.controls.add("scoreboard_sidebar_score", this.number());
        this.controls.add("scoreboard_sidebar/main/displayed_objective", this.line(true));
        JsonObject scores = new JsonObject();
        scores.addProperty("collection_name", "scoreboard_players");
        this.controls.add("scoreboard_sidebar/main/lists/scores", scores);
        JsonArray overlays = new JsonArray();
        StringBuilder offsetX = new StringBuilder();
        for (JsonElement value : this.profiles) {
            JsonObject profile = value.getAsJsonObject();
            int index = profile.get("index").getAsInt();
            String match = "(('%.18s' * #ce_raw) = '" + SidebarCodec.MARKER + "§r§r" + String.format(Locale.ROOT, "P%04d:", index) + "')";
            int offset = profile.get("offset-x").getAsInt();
            if (offset != 0) {
                offsetX.append(" + (").append(match).append(" * ").append(offset).append(")");
            }
            for (JsonElement element : profile.getAsJsonArray("elements")) {
                JsonObject image = element.getAsJsonObject();
                JsonObject control = new JsonObject();
                control.addProperty("type", "image");
                control.addProperty("texture", image.get("texture").getAsString());
                control.addProperty("anchor_from", image.get("anchor").getAsString());
                control.addProperty("anchor_to", image.get("anchor").getAsString());
                control.addProperty("layer", image.get("layer").getAsInt());
                control.add("size", GsonHelper.createArray(image.get("width").getAsInt(), image.get("height").getAsInt()));
                control.add("offset", GsonHelper.createArray(image.get("x").getAsInt(), image.get("y").getAsInt()));
                JsonArray bindings = new JsonArray();
                bindings.add(this.input("#objective_sidebar_name", "", "#ce_raw"));
                bind(bindings, match, "#visible");
                control.add("bindings", bindings);
                JsonObject anchor = new JsonObject();
                anchor.addProperty("type", "panel");
                // Only the zero-sized anchor participates in sidebar measurement; the image may extend beyond it.
                anchor.add("size", GsonHelper.createArray(0, 0));
                anchor.addProperty("anchor_from", image.get("anchor").getAsString());
                anchor.addProperty("anchor_to", image.get("anchor").getAsString());
                JsonArray children = new JsonArray();
                children.add(GsonHelper.createObject("image", control));
                anchor.add("controls", children);
                overlays.add(GsonHelper.createObject("ce_" + index + "_" + image.get("id").getAsString(), anchor));
            }
        }
        JsonObject sidebar = new JsonObject();
        if (!overlays.isEmpty()) {
            JsonObject modification = new JsonObject();
            modification.addProperty("array_name", "controls");
            modification.addProperty("operation", "insert_front");
            modification.add("value", overlays);
            JsonArray modifications = new JsonArray();
            modifications.add(modification);
            sidebar.add("modifications", modifications);
        }
        if (!sidebar.isEmpty()) {
            this.controls.add("scoreboard_sidebar", sidebar);
        }
        Files.createDirectories(root.resolve("ui"));
        GsonHelper.writeJsonFile(this.controls, root.resolve("ui/scoreboards.json"));
        if (!offsetX.isEmpty()) {
            JsonObject position = new JsonObject();
            position.addProperty("use_anchored_offset", true);
            JsonObject properties = new JsonObject();
            properties.addProperty("#anchored_offset_value_x", 0);
            position.add("property_bag", properties);
            JsonArray offsetBindings = new JsonArray();
            offsetBindings.add(this.input("#objective_sidebar_name", "", "#ce_raw"));
            bind(offsetBindings, "(0" + offsetX + ")", "#anchored_offset_value_x");
            position.add("bindings", offsetBindings);
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("type", "panel");
            // Anchored offsets are relative to the parent size; a 1x1 parent makes them UI pixels.
            wrapper.add("size", GsonHelper.createArray(1, 1));
            wrapper.addProperty("anchor_from", "right_middle");
            wrapper.addProperty("anchor_to", "right_middle");
            JsonArray visibility = new JsonArray();
            visibility.add(this.input("#scoreboard_sidebar_visible", "", "#visible"));
            wrapper.add("bindings", visibility);
            JsonArray children = new JsonArray();
            children.add(GsonHelper.createObject("content@scoreboard.scoreboard_sidebar", position));
            wrapper.add("controls", children);
            JsonObject modification = new JsonObject();
            modification.addProperty("array_name", "controls");
            modification.addProperty("control_name", "sidebar");
            modification.addProperty("operation", "replace");
            modification.add("value", GsonHelper.createObject("sidebar", wrapper));
            JsonArray modifications = new JsonArray();
            modifications.add(modification);
            JsonObject rootPanel = new JsonObject();
            rootPanel.add("modifications", modifications);
            JsonObject hud = new JsonObject();
            hud.addProperty("namespace", "hud");
            hud.add("root_panel", rootPanel);
            GsonHelper.writeJsonFile(hud, root.resolve("ui/hud_screen.json"));
        }
    }

    private JsonObject line(boolean title) {
        String input = title ? "#objective_sidebar_name" : "#player_name_sidebar";
        String collection = title ? "" : "scoreboard_players";
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("size", size("100%cm", 10));
        panel.addProperty("anchor_from", title ? "top_middle" : "top_left");
        panel.addProperty("anchor_to", title ? "top_middle" : "top_left");
        JsonArray children = new JsonArray();
        JsonObject nativeLabel = label();
        nativeLabel.addProperty("text", "#ce_native");
        nativeLabel.add("max_size", GsonHelper.createArray(250, 10));
        JsonArray nativeBindings = new JsonArray();
        nativeBindings.add(fromSibling("sidebar_layers", "#ce_native", "#ce_native"));
        nativeBindings.add(fromSibling("sidebar_layers", "(not #ce_encoded)", "#visible"));
        nativeLabel.add("bindings", nativeBindings);
        children.add(GsonHelper.createObject("native_text", nativeLabel));
        JsonObject measure = label();
        measure.addProperty("alpha", 0);
        measure.addProperty("text", "#ce_measure");
        JsonArray measureBindings = new JsonArray();
        measureBindings.add(fromSibling("sidebar_layers", "#ce_measure", "#ce_measure"));
        measureBindings.add(fromSibling("sidebar_layers", "#ce_encoded", "#visible"));
        measure.add("bindings", measureBindings);
        children.add(GsonHelper.createObject("measure", measure));
        JsonObject layers = new JsonObject();
        layers.addProperty("type", "stack_panel");
        layers.addProperty("orientation", "horizontal");
        layers.addProperty("anchor_from", "top_left");
        layers.addProperty("anchor_to", "top_left");
        layers.add("offset", GsonHelper.createArray(-ContainerTitleLayout.ORIGIN, 0));
        layers.add("size", GsonHelper.createArray(0, 0));
        layers.addProperty("collection_name", "ce_sidebar_layers");
        JsonObject factory = new JsonObject();
        factory.addProperty("name", "ce_sidebar_layers_factory");
        factory.addProperty("control_name", title ? "scoreboard.ce_sidebar_title_layer" : "scoreboard.ce_sidebar_player_layer");
        factory.addProperty("max_children_size", ContainerTitleCodec.MAX_BYTES / ContainerTitleCodec.ENTRY_BYTES);
        layers.add("factory", factory);
        JsonArray bindings = source(input, collection);
        bind(bindings, "(('%.' + ((not #ce_encoded) * 10032) + 's') * #ce_raw)", "#ce_native");
        decode(bindings, null, "'%.7s'", "#ce_measure");
        bind(bindings, "(#ce_header - ('%." + SidebarCodec.PREFIX_BYTES + "s' * #ce_header))", "#ce_count");
        bind(bindings, "((#ce_count - 'N' - ':' + 0 - 2) * #ce_encoded)", "#collection_length");
        layers.add("bindings", bindings);
        children.add(GsonHelper.createObject("sidebar_layers", layers));
        panel.add("controls", children);
        return panel;
    }

    private JsonObject number() {
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("size", size("100%cm", 10));
        panel.addProperty("anchor_from", "top_right");
        panel.addProperty("anchor_to", "top_right");
        JsonArray children = new JsonArray();
        JsonObject original = label();
        original.addProperty("text", "#ce_native");
        original.addProperty("color", "$player_score_color");
        JsonArray bindings = new JsonArray();
        bindings.add(fromSibling("formatted_score", "#ce_encoded", "#ce_encoded"));
        bindings.add(this.input("#player_score_sidebar", "scoreboard_scores", "#player_score_sidebar"));
        bind(bindings, "(('%.' + ((not #ce_encoded) * 10032) + 's') * #player_score_sidebar)", "#ce_native");
        bind(bindings, "(not #ce_encoded)", "#visible");
        original.add("bindings", bindings);
        children.add(GsonHelper.createObject("native_score", original));
        JsonObject formatted = label();
        bindings = source("#player_name_sidebar", "scoreboard_players");
        decode(bindings, "'%.14s'", "'%.21s'", "#ce_score");
        formatted.addProperty("text", "#ce_score");
        bind(bindings, "#ce_encoded", "#visible");
        formatted.add("bindings", bindings);
        children.add(GsonHelper.createObject("formatted_score", formatted));
        panel.add("controls", children);
        return panel;
    }

    private JsonObject layer(boolean title) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "panel");
        item.add("size", GsonHelper.createArray(0, 0));
        JsonObject text = label();
        text.addProperty("text", "#ce_slice");
        JsonArray bindings = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("binding_type", "collection_details");
        details.addProperty("binding_collection_name", "ce_sidebar_layers");
        details.addProperty("binding_collection_prefix", "ce");
        bindings.add(details);
        // A factory entry must read its own scoreboard row; named controls can resolve to a different row.
        bindings.add(this.input(title ? "#objective_sidebar_name" : "#player_name_sidebar", title ? "" : "scoreboard_players", "#ce_raw"));
        bind(bindings, "(#ce_raw - ('%." + HEADER_BYTES + "s' * #ce_raw))", "#ce_payload");
        String offset = "((#ce_collection_index + 2) * " + ContainerTitleCodec.ENTRY_BYTES + ")";
        decode(bindings, "('%.' + " + offset + " + 's')", "('%.' + (" + offset + " + " + ContainerTitleCodec.FORMAT_BYTES + ") + 's')", "#ce_slice");
        text.add("bindings", bindings);
        JsonArray children = new JsonArray();
        children.add(GsonHelper.createObject("text", text));
        item.add("controls", children);
        return item;
    }

    private JsonArray source(String input, String collection) {
        JsonArray bindings = new JsonArray();
        bindings.add(this.input(input, collection, "#ce_raw"));
        bind(bindings, "(('%.6s' * #ce_raw) = '" + SidebarCodec.MARKER + "')", "#ce_encoded");
        bind(bindings, "('%." + HEADER_BYTES + "s' * #ce_raw)", "#ce_header");
        bind(bindings, "((('%.' + (#ce_encoded * 10032) + 's') * #ce_raw) - #ce_header)", "#ce_payload");
        return bindings;
    }

    private JsonObject input(String name, String collection, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", collection.isEmpty() ? "global" : "collection");
        binding.addProperty("binding_name", name);
        binding.addProperty("binding_name_override", target);
        if (!collection.isEmpty()) {
            binding.addProperty("binding_collection_name", collection);
        }
        return binding;
    }

    private static void decode(JsonArray bindings, String start, String middle, String target) {
        String entry = start == null ? "#ce_payload" : "(#ce_payload - (" + start + " * #ce_payload))";
        bind(bindings, "('%.7s' * " + entry + ")", "#ce_skip");
        bind(bindings, "('%.7s' * (#ce_payload - (" + middle + " * #ce_payload)))", "#ce_take");
        bind(bindings, "(#ce_take * (#ce_payload - (#ce_skip * #ce_payload)))", target);
    }

    private static JsonObject fromSibling(String control, String source, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "view");
        binding.addProperty("source_control_name", control);
        binding.addProperty("source_property_name", source);
        binding.addProperty("target_property_name", target);
        binding.addProperty("resolve_sibling_scope", true);
        return binding;
    }

    private static void bind(JsonArray bindings, String expression, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "view");
        binding.addProperty("source_property_name", expression);
        binding.addProperty("target_property_name", target);
        bindings.add(binding);
    }

    private static JsonObject label() {
        JsonObject label = new JsonObject();
        label.addProperty("type", "label");
        label.add("size", size("default", 10));
        label.addProperty("anchor_from", "top_left");
        label.addProperty("anchor_to", "top_left");
        label.addProperty("text_alignment", "left");
        label.addProperty("localize", false);
        label.addProperty("shadow", true);
        label.addProperty("layer", 2);
        return label;
    }

    private static JsonArray size(String width, int height) {
        JsonArray result = new JsonArray();
        result.add(width);
        result.add(height);
        return result;
    }
}
