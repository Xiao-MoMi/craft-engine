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
