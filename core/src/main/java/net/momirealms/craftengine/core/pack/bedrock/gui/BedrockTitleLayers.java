package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

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

    static JsonArray layerBindings(String collection, String sourceControl) {
        JsonArray bindings = collectionBindings(collection);
        bindings.add(fromControl(sourceControl, "#ce_payload", "#ce_payload"));
        bind(bindings, "(#ce_collection_index * " + ContainerTitleCodec.ENTRY_BYTES + ")", "#ce_entry_start");
        String beforeEntry = "(('%.' + #ce_entry_start + 's') * #ce_payload)";
        String throughEntry = "(('%.' + (#ce_entry_start + " + ContainerTitleCodec.ENTRY_BYTES + ") + 's') * #ce_payload)";
        // 索引表只含 ASCII 格式串。加上表中没有的 ^，让字符串减法只删除开头一次。
        bind(bindings, "(('^' + " + throughEntry + ") - ('^' + " + beforeEntry + "))", "#ce_entry");
        bind(bindings, "('%." + ContainerTitleCodec.FORMAT_BYTES + "s' * #ce_entry)", "#ce_skip");
        bind(bindings, "(('^' + #ce_entry) - ('^' + #ce_skip))", "#ce_take");
        bind(bindings, "(#ce_take * (#ce_payload - (#ce_skip * #ce_payload)))", "#ce_line");
        return bindings;
    }

    static JsonObject fromControl(String control, String source, String target) {
        JsonObject binding = new JsonObject();
        binding.addProperty("binding_type", "view");
        binding.addProperty("source_control_name", control);
        binding.addProperty("source_property_name", source);
        binding.addProperty("target_property_name", target);
        return binding;
    }

    public static JsonObject positionedLayer(JsonObject label, JsonArray bindings, boolean container) {
        return positionedLayer(label, bindings, 1, container, null);
    }

    static JsonObject titleLayer(JsonObject label, JsonArray bindings, int scale, JsonObject background) {
        JsonObject plain = label.deepCopy();
        JsonArray children = new JsonArray(1);
        children.add(GsonHelper.createObject("background", background));
        plain.add("controls", children);
        return positionedLayer(label, bindings, scale, false, plain);
    }

    private static JsonObject positionedLayer(JsonObject label, JsonArray bindings, int scale, boolean container, @Nullable JsonObject plain) {
        int markerEnd = ContainerTitleCodec.POSITION_MARKER_BYTES;
        int xEnd = markerEnd + ContainerTitleCodec.X_BYTES;
        int yEnd = xEnd + ContainerTitleCodec.Y_BYTES;
        bind(bindings, prefix(markerEnd), "#ce_line_marker");
        String unicodeOverride = "(#ce_line_marker = '" + ContainerTitleCodec.UNICODE_SHADOW_OVERRIDE_MARKER + "')";
        bind(bindings, "((#ce_line_marker = '" + ContainerTitleCodec.UNICODE_MARKER + "') or " + unicodeOverride + ")", "#ce_unicode");
        bind(bindings, "((#ce_line_marker = '" + ContainerTitleCodec.SHADOW_OVERRIDE_MARKER + "') or " + unicodeOverride + ")", "#ce_shadow_override");
        bind(bindings, "(#ce_unicode or (#ce_shadow_override or (#ce_line_marker = '" + ContainerTitleCodec.POSITION_MARKER + "')))", "#ce_positioned");
        bind(bindings, "(" + prefix(xEnd) + " - " + prefix(markerEnd) + " + 0)", "#ce_x");
        bind(bindings, "($ce_half_scale * (#ce_x - " + ContainerTitleCodec.X_BIAS + "))", "#ce_offset_x");
        bind(bindings, "(" + prefix(yEnd) + " - " + prefix(xEnd) + " + 0)", "#ce_y");
        bind(bindings, "($ce_y_scale * #ce_y)", "#ce_offset_y");
        bind(bindings, "(('^' + #ce_line) - ('^' + " + prefix(yEnd) + "))", "#ce_ink_text");

        JsonObject data = panel(0);
        data.addProperty("$ce_half_scale", scale * 0.5);
        data.addProperty("$ce_y_scale", -(double) scale);
        data.add("bindings", bindings);
        label.addProperty("anchor_from", "top_left");
        label.addProperty("anchor_to", container ? "top_left" : "left_middle");
        label.addProperty("text_alignment", "left");
        JsonArray controls = faceControls(data, label, true);
        if (plain != null) {
            // Title 和 Subtitle 可以在同一帧中混用原生文字和定位图层。原生标签从零宽工厂项的原点居中。
            plain.addProperty("anchor_from", "top_left");
            plain.addProperty("anchor_to", "center");
            plain.addProperty("text", "#text");
            JsonArray plainBindings = new JsonArray(2);
            plainBindings.add(fromSibling("ce_layer_data", "#ce_line", "#text"));
            plainBindings.add(fromSibling("ce_layer_data", "(not #ce_positioned)", "#visible"));
            plain.add("bindings", plainBindings);
            controls.add(GsonHelper.createObject("plain_text", plain));
        }

        JsonObject origin = panel(1);
        origin.addProperty("anchor_from", "top_left");
        origin.addProperty("anchor_to", "top_left");
        if (container) {
            origin.add("offset", GsonHelper.createArray(ContainerTitleLayout.ORIGIN, 0));
        }
        origin.add("controls", controls);
        JsonArray children = new JsonArray(1);
        children.add(GsonHelper.createObject("origin", origin));
        JsonObject item = panel(0);
        item.add("controls", children);
        return item;
    }

    private static String prefix(int bytes) {
        return "('%." + bytes + "s' * #ce_line)";
    }

    static JsonArray faceControls(JsonObject data, JsonObject label, boolean verticalOffset) {
        // 控件名称会在每个工厂项内重复。只从同级数据控件取值，避免读到其他图层或计分板行。
        JsonArray controls = new JsonArray(5);
        JsonObject offsets = new JsonObject();
        offsets.addProperty("#ce_offset_x", 0.0);
        if (verticalOffset) {
            offsets.addProperty("#ce_offset_y", 0.0);
        }
        data.add("property_bag", offsets);
        controls.add(GsonHelper.createObject("ce_layer_data", data));
        label.addProperty("text", "#text");
        label.addProperty("use_anchored_offset", true);
        JsonObject properties = new JsonObject();
        properties.addProperty("#anchored_offset_value_x", 1.0);
        properties.addProperty("#anchored_offset_value_y", verticalOffset ? 1.0 : 0.0);
        label.add("property_bag", properties);
        JsonArray bindings = new JsonArray(4);
        bindings.add(fromSibling("ce_layer_data", "#ce_offset_x", "#anchored_offset_value_x"));
        if (verticalOffset) {
            bindings.add(fromSibling("ce_layer_data", "#ce_offset_y", "#anchored_offset_value_y"));
        }
        bindings.add(fromSibling("ce_layer_data", "#ce_ink_text", "#text"));
        label.add("bindings", bindings);
        boolean defaultShadow = label.get("shadow").getAsBoolean();
        addLabel(controls, label, "text", "default", defaultShadow, "((not #ce_unicode) and (not #ce_shadow_override))");
        addLabel(controls, label, "unicode_text", "unicode", defaultShadow, "(#ce_unicode and (not #ce_shadow_override))");
        addLabel(controls, label, "shadow_override_text", "default", !defaultShadow, "((not #ce_unicode) and #ce_shadow_override)");
        addLabel(controls, label, "unicode_shadow_override_text", "unicode", !defaultShadow, "(#ce_unicode and #ce_shadow_override)");
        return controls;
    }

    private static void addLabel(JsonArray controls, JsonObject template, String name, String font, boolean shadow, String visible) {
        JsonObject label = template.deepCopy();
        label.addProperty("font_type", font);
        label.addProperty("shadow", shadow);
        label.getAsJsonArray("bindings").add(fromSibling("ce_layer_data", "(#ce_positioned and " + visible + ")", "#visible"));
        controls.add(GsonHelper.createObject(name, label));
    }

    public static JsonObject fromSibling(String control, String source, String target) {
        JsonObject binding = fromControl(control, source, target);
        binding.addProperty("resolve_sibling_scope", true);
        return binding;
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
