package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;

final class BedrockTitleLayers {
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
        decodeLayer(bindings);
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

    static JsonArray collectionBindings(String collection) {
        JsonArray bindings = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("binding_type", "collection_details");
        details.addProperty("binding_collection_name", collection);
        details.addProperty("binding_collection_prefix", "ce");
        bindings.add(details);
        return bindings;
    }

    static void decodeLayer(JsonArray bindings) {
        // 每层从索引表取出 UTF-8 偏移和长度，再从正文截取对应文本。
        bind(bindings, "('%.' + (#ce_collection_index * " + ContainerTitleCodec.ENTRY_BYTES + ") + 's')", "#ce_skip_format");
        bind(bindings, "(#ce_skip_format * #ce_payload)", "#ce_skip_prefix");
        bind(bindings, "(#ce_payload - #ce_skip_prefix)", "#ce_skip_tail");
        bind(bindings, "('%." + ContainerTitleCodec.FORMAT_BYTES + "s' * #ce_skip_tail)", "#ce_skip");
        bind(bindings, "('%.' + ((#ce_collection_index * " + ContainerTitleCodec.ENTRY_BYTES + ") + " + ContainerTitleCodec.FORMAT_BYTES + ") + 's')", "#ce_take_format");
        bind(bindings, "(#ce_take_format * #ce_payload)", "#ce_take_prefix");
        bind(bindings, "(#ce_payload - #ce_take_prefix)", "#ce_take_tail");
        bind(bindings, "('%." + ContainerTitleCodec.FORMAT_BYTES + "s' * #ce_take_tail)", "#ce_take");
        bind(bindings, "(#ce_skip * #ce_payload)", "#ce_body_prefix");
        bind(bindings, "(#ce_payload - #ce_body_prefix)", "#ce_body_tail");
        bind(bindings, "(#ce_take * #ce_body_tail)", "#ce_layer");
    }
}
