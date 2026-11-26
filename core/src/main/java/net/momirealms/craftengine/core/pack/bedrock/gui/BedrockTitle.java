package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.TitleFrame;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitleLayers.bind;
import static net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitleLayers.fromControl;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

public final class BedrockTitle {
    private static final String FRAME = "#hud_subtitle_text_string";
    private static final String HOST = "ce_title_host";

    private BedrockTitle() {
    }

    public static void write(Path root, int layer) throws IOException {
        Path path = root.resolve("ui/hud_screen.json");
        Files.createDirectories(path.getParent());
        JsonObject hud = Files.isRegularFile(path) ? GsonHelper.readJsonObjectFromFile(path) : new JsonObject();
        hud.addProperty("namespace", "hud");
        BedrockActionBar.add(hud);
        JsonArray nativeBindings = children(global("#hud_title_text_string", "#ce_display"));
        bind(nativeBindings, "(not (#ce_display = '" + TitleFrame.DISPLAY + "'))", "#visible");
        hud.add("hud_title_text", createObject("bindings", nativeBindings));

        JsonObject host = new JsonObject();
        host.addProperty("type", "panel");
        host.addProperty("layer", layer);
        host.addProperty("propagate_alpha", true);
        host.add("controls", children(channel(hud, false), channel(hud, true)));
        // A float property preserves fractional opacity in Bedrock's view expressions.
        JsonObject properties = new JsonObject();
        properties.addProperty("#ce_alpha_scale", 1.0 / 255.0);
        host.add("property_bag", properties);
        JsonArray active = children(global("#hud_title_text_string", "#ce_display"), global(FRAME, "#ce_wire"));
        // Factory children read the current frame from this persistent control, including on their first render.
        bind(active, "(#ce_wire + '^')", "#ce_frame");
        bind(active, "(#ce_display = '" + TitleFrame.DISPLAY + "')", "#ce_marker");
        bind(active, "(('%.1s' * #ce_wire) = 'A')", "#ce_has_frame");
        bind(active, "('%.4s' * #ce_wire)", "#ce_alpha_header");
        bind(active, "(#ce_alpha_scale * (#ce_alpha_header - 'A' + 0))", "#alpha");
        bind(active, "(#ce_marker and #ce_has_frame)", "#ce_active");
        bind(active, "(#ce_active * " + capacity(false) + ")", "#ce_title_capacity");
        bind(active, "(#ce_active * " + capacity(true) + ")", "#ce_subtitle_capacity");
        bind(active, "(" + prefix(TitleFrame.HEADER_BYTES - 1) + " - " + prefix(TitleFrame.HEADER_BYTES - 2) + " = 'P')", "#ce_title_background");
        bind(active, "(" + prefix(TitleFrame.HEADER_BYTES) + " - " + prefix(TitleFrame.HEADER_BYTES - 1) + " = 'P')", "#ce_subtitle_background");
        host.add("bindings", active);
        JsonObject append = new JsonObject();
        append.addProperty("array_name", "controls");
        append.addProperty("operation", "insert_back");
        append.add("value", children(createObject(HOST, host)));
        JsonObject rootPanel = hud.getAsJsonObject("root_panel");
        if (rootPanel == null) {
            rootPanel = new JsonObject();
            hud.add("root_panel", rootPanel);
        }
        JsonArray modifications = rootPanel.getAsJsonArray("modifications");
        if (modifications == null) {
            modifications = new JsonArray();
            rootPanel.add("modifications", modifications);
        }
        modifications.add(append);

        GsonHelper.writeJsonFile(hud, path);
    }

    private static JsonObject channel(JsonObject hud, boolean subtitle) {
        String collection = subtitle ? "ce_subtitle_display_layers" : "ce_title_display_layers";
        String template = collection + "_entry";
        JsonObject factory = new JsonObject();
        factory.addProperty("name", collection + "_factory");
        factory.addProperty("control_name", "hud." + template);
        factory.addProperty("max_children_size", ContainerTitleCodec.MAX_BYTES / ContainerTitleCodec.ENTRY_BYTES);
        JsonObject layers = new JsonObject();
        layers.addProperty("type", "stack_panel");
        layers.addProperty("orientation", "horizontal");
        layers.add("size", GsonHelper.createArray(0, 0));
        layers.add("offset", GsonHelper.createArray(subtitle ? 1 : 0, subtitle ? 7 : -29));
        layers.addProperty("collection_name", collection);
        layers.add("factory", factory);
        layers.add("bindings", children(fromControl(HOST, subtitle ? "#ce_subtitle_capacity" : "#ce_title_capacity", "#collection_length")));

        JsonArray bindings = BedrockTitleLayers.collectionBindings(collection);
        bindings.add(fromControl(HOST, "#ce_frame", "#ce_frame"));
        String index = "#ce_collection_index";
        if (subtitle) {
            bindings.add(fromControl(HOST, "#ce_title_capacity", "#ce_title_capacity"));
            index = "(#ce_title_capacity + #ce_collection_index)";
        }
        // Extract both endpoints from the same frame before slicing the layer, including an empty slot.
        bind(bindings, "((" + index + " * " + TitleFrame.ENTRY_BYTES + ") + " + TitleFrame.HEADER_BYTES + ")", "#ce_start");
        bind(bindings, "(#ce_start + " + TitleFrame.FORMAT_BYTES + ")", "#ce_middle");
        bind(bindings, "(#ce_middle + " + TitleFrame.FORMAT_BYTES + ")", "#ce_end");
        for (String part : new String[]{"start", "middle", "end"}) {
            bind(bindings, "(('%.' + #ce_" + part + " + 's') * #ce_frame)", "#ce_" + part + "_prefix");
        }
        bind(bindings, "(#ce_middle_prefix - #ce_start_prefix)", "#ce_from");
        bind(bindings, "(#ce_end_prefix - #ce_middle_prefix)", "#ce_to");
        bind(bindings, "(#ce_from * #ce_frame)", "#ce_before");
        bind(bindings, "(#ce_to * #ce_frame)", "#ce_through");
        bind(bindings, "(('^' + #ce_through) - ('^' + #ce_before))", "#text");

        JsonObject label = new JsonObject();
        label.addProperty("type", "label");
        label.addProperty("text", "#text");
        label.addProperty("localize", false);
        label.addProperty("text_alignment", "center");
        label.addProperty("font_size", subtitle ? "large" : "extra_large");
        label.addProperty("color", "$title_command_text_color");
        label.addProperty("shadow", true);
        label.add("bindings", bindings);

        JsonObject background = new JsonObject();
        background.addProperty("type", "image");
        background.addProperty("texture", "textures/ui/hud_tip_text_background");
        background.addProperty("layer", -1);
        background.addProperty("alpha", 0.5);
        JsonArray size = new JsonArray();
        size.add(subtitle ? "100% + 20px" : "100% + 30px");
        size.add(subtitle ? "100% + 5px" : "100% + 6px");
        background.add("size", size);
        JsonArray plain = BedrockTitleLayers.collectionBindings(collection);
        plain.add(fromControl(HOST, subtitle ? "#ce_subtitle_background" : "#ce_title_background", "#ce_plain"));
        bind(plain, "(#ce_plain and (#ce_collection_index = 0))", "#visible");
        background.add("bindings", plain);
        label.add("controls", children(createObject("background", background)));

        hud.add(template, BedrockTitleLayers.positionedLayer(label, "#text", subtitle ? 2 : 4, false));
        return createObject(collection, layers);
    }

    private static String capacity(boolean subtitle) {
        int end = subtitle ? TitleFrame.SUBTITLE_CAPACITY_END : TitleFrame.TITLE_CAPACITY_END;
        return "(" + prefix(end) + " - " + prefix(end - 5) + " - '" + (subtitle ? "S" : "T") + "' + 0)";
    }

    private static String prefix(int end) {
        return "('%." + end + "s' * #ce_frame)";
    }

    private static JsonObject global(String input, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "global");
        binding.addProperty("binding_name", input);
        binding.addProperty("binding_name_override", target);
        return binding;
    }

    private static JsonArray children(JsonObject... controls) {
        JsonArray array = new JsonArray();
        for (JsonObject control : controls) {
            array.add(control);
        }
        return array;
    }
}
