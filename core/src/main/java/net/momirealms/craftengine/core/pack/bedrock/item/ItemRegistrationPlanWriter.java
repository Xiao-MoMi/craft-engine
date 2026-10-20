package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.IOException;
import java.nio.file.Path;

public final class ItemRegistrationPlanWriter {

    private ItemRegistrationPlanWriter() {
    }

    public static void write(ItemRegistrationPlan plan, Path path) throws IOException {
        GsonHelper.writeJsonFile(toJson(plan), path);
    }

    public static JsonObject toJson(ItemRegistrationPlan plan) {
        JsonObject result = new JsonObject();
        result.addProperty("kind", "craftengine:item_registration_plan");
        result.addProperty("format_version", 1);
        result.addProperty("source_version", VersionHelper.MINECRAFT_VERSION.version());
        result.addProperty("target_geyser_api", ItemRegistrationPlanCompiler.TARGET_API);
        result.addProperty("has_errors", plan.hasErrors());
        JsonArray entries = new JsonArray();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            JsonObject json = new JsonObject();
            json.addProperty("bedrock_identifier", entry.bedrockId().asString());
            json.addProperty("java_item", entry.javaItemType().asString());
            json.addProperty("model", entry.definitionId().asString());
            json.addProperty("definition_file", ResourcePackUtils.itemDefinitionPath(entry.definitionId()));
            json.add("component_snapshot", GsonHelper.get().toJsonTree(entry.properties().components()));
            json.add("component_overrides", GsonHelper.get().toJsonTree(entry.componentOverrides()));
            JsonArray removed = new JsonArray();
            entry.removedComponents().stream().map(Object::toString).sorted().forEach(removed::add);
            json.add("removed_components", removed);
            JsonObject resources = new JsonObject();
            resources.addProperty("icon", entry.icon());
            resources.addProperty("attachable_identifier", entry.bedrockId().asString());
            resources.addProperty("generated", false);
            resources.addProperty("model_rule_count", entry.definition().rules().size());
            json.add("resources", resources);
            ResolvedItemBinding binding = entry.source();
            JsonObject source = new JsonObject();
            source.addProperty("item_id", binding.itemId().asString());
            source.addProperty("server_item_type", binding.serverItemType().asString());
            source.addProperty("client_item_type", binding.clientItemType().asString());
            source.add("client_behavior", GsonHelper.get().toJsonTree(binding.clientBehavior()));
            json.add("source", source);
            entries.add(json);
        }
        result.add("entries", entries);
        JsonArray diagnostics = new JsonArray();
        for (ItemRegistrationPlan.Diagnostic diagnostic : plan.diagnostics()) {
            JsonObject json = new JsonObject();
            json.addProperty("severity", diagnostic.severity().name());
            json.addProperty("code", diagnostic.code());
            json.addProperty("java_item", diagnostic.javaItemType().asString());
            if (diagnostic.definitionId() != null) {
                json.addProperty("model", diagnostic.definitionId().asString());
                json.addProperty("definition_file", ResourcePackUtils.itemDefinitionPath(diagnostic.definitionId()));
            }
            JsonArray items = new JsonArray();
            diagnostic.itemIds().forEach(id -> items.add(id.asString()));
            json.add("item_ids", items);
            json.add("details", GsonHelper.get().toJsonTree(diagnostic.details()));
            diagnostics.add(json);
        }
        result.add("diagnostics", diagnostics);
        return GsonHelper.sortObjectKeys(result).getAsJsonObject();
    }
}
