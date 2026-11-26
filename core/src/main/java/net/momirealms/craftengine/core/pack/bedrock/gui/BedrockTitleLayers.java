package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.util.GsonHelper;

public final class BedrockTitleLayers {
    private BedrockTitleLayers() {
    }

    static void bind(JsonArray bindings, String expression, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "view");
        binding.addProperty("source_property_name", expression);
        binding.addProperty("target_property_name", target);
        bindings.add(binding);
    }

    static void bindLayer(JsonObject label, String collection, String sourceControl) {
        label.addProperty("text", "#ce_layer");
        JsonArray bindings = collectionBindings(collection);
        bindings.add(fromControl(sourceControl, "#ce_payload", "#ce_payload"));
        bind(bindings, "(#ce_collection_index * " + ContainerTitleCodec.ENTRY_BYTES + ")", "#ce_entry_start");
        String beforeEntry = "(('%.' + #ce_entry_start + 's') * #ce_payload)";
        String throughEntry = "(('%.' + (#ce_entry_start + " + ContainerTitleCodec.ENTRY_BYTES + ") + 's') * #ce_payload)";
        // 索引表只含 ASCII 格式串。加上表中没有的 ^，让字符串减法只删除开头一次。
        bind(bindings, "(('^' + " + throughEntry + ") - ('^' + " + beforeEntry + "))", "#ce_entry");
        bind(bindings, "('%." + ContainerTitleCodec.FORMAT_BYTES + "s' * #ce_entry)", "#ce_skip");
        bind(bindings, "(('^' + #ce_entry) - ('^' + #ce_skip))", "#ce_take");
        bind(bindings, "(#ce_take * (#ce_payload - (#ce_skip * #ce_payload)))", "#ce_layer");
        label.add("bindings", bindings);
    }

    static JsonObject fromControl(String control, String source, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "view");
        binding.addProperty("source_control_name", control);
        binding.addProperty("source_property_name", source);
        binding.addProperty("target_property_name", target);
        return binding;
    }

    public static JsonObject positionedLayer(JsonObject label, String raw, int scale, boolean container) {
        JsonArray input = label.getAsJsonArray("bindings");
        input.get(input.size() - 1).getAsJsonObject().addProperty("target_property_name", "#ce_line");
        int markerEnd = ContainerTitleCodec.POSITION_MARKER_BYTES;
        int xEnd = markerEnd + ContainerTitleCodec.X_BYTES;
        int yEnd = xEnd + ContainerTitleCodec.Y_BYTES;
        bind(input, "(" + prefix(markerEnd) + " = '" + ContainerTitleCodec.UNICODE_MARKER + "')", "#ce_unicode");
        bind(input, "(#ce_unicode or (" + prefix(markerEnd) + " = '" + ContainerTitleCodec.POSITION_MARKER + "'))", "#ce_positioned");

        JsonObject ink = label.deepCopy();
        ink.remove("controls");
        ink.addProperty("text", raw);
        ink.addProperty("anchor_from", "top_left");
        ink.addProperty("anchor_to", container ? "top_left" : "left_middle");
        ink.addProperty("text_alignment", "left");
        JsonArray inkBindings = ink.getAsJsonArray("bindings");
        bind(inkBindings, "(('^' + #ce_line) - ('^' + " + prefix(yEnd) + "))", raw);

        JsonArray positionBindings = input.deepCopy();
        bind(positionBindings, "#ce_positioned", "#visible");
        bind(positionBindings, "(" + prefix(xEnd) + " - " + prefix(markerEnd) + " + 0)", "#ce_x");
        bind(positionBindings, "($ce_half_scale * (#ce_x - " + ContainerTitleCodec.X_BIAS + "))", "#anchored_offset_value_x");
        bind(positionBindings, "(" + prefix(yEnd) + " - " + prefix(xEnd) + " + 0)", "#ce_y");
        bind(positionBindings, "($ce_y_scale * #ce_y)", "#anchored_offset_value_y");

        JsonObject position = panel(1);
        position.addProperty("anchor_from", "top_left");
        position.addProperty("anchor_to", "top_left");
        position.addProperty("use_anchored_offset", true);
        position.addProperty("$ce_half_scale", scale * 0.5);
        position.addProperty("$ce_y_scale", -(double) scale);
        JsonObject properties = new JsonObject();
        properties.addProperty("#anchored_offset_value_x", 1.0);
        properties.addProperty("#anchored_offset_value_y", 1.0);
        position.add("property_bag", properties);
        position.add("bindings", positionBindings);
        JsonArray inkControls = new JsonArray();
        addFaceLabels(inkControls, ink);
        position.add("controls", inkControls);

        JsonObject origin = panel(1);
        origin.addProperty("anchor_from", "top_left");
        origin.addProperty("anchor_to", "top_left");
        if (container) {
            origin.add("offset", GsonHelper.createArray(ContainerTitleLayout.ORIGIN, 0));
        }
        JsonArray positioned = new JsonArray();
        positioned.add(GsonHelper.createObject("position", position));
        origin.add("controls", positioned);
        bind(input, "#ce_line", raw);
        bind(input, "(not #ce_positioned)", "#visible");
        JsonArray children = new JsonArray();
        children.add(GsonHelper.createObject("legacy", label));
        children.add(GsonHelper.createObject("origin", origin));
        JsonObject item = panel(0);
        item.add("controls", children);
        return item;
    }

    private static String prefix(int bytes) {
        return "('%." + bytes + "s' * #ce_line)";
    }

    static void addFaceLabels(JsonArray controls, JsonObject label) {
        JsonObject unicode = label.deepCopy();
        unicode.addProperty("font_type", "unicode");
        bind(unicode.getAsJsonArray("bindings"), "#ce_unicode", "#visible");
        label.addProperty("font_type", "default");
        bind(label.getAsJsonArray("bindings"), "(not #ce_unicode)", "#visible");
        controls.add(GsonHelper.createObject("text", label));
        controls.add(GsonHelper.createObject("unicode_text", unicode));
    }

    private static JsonObject panel(int size) {
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("size", GsonHelper.createArray(size, size));
        return panel;
    }

    static JsonArray collectionBindings(String collection) {
        JsonArray bindings = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("binding_type", "collection_details");
        details.addProperty("binding_collection_name", collection);
        details.addProperty("binding_collection_prefix", "ce");
        bindings.add(details);
        return bindings;
    }
}
