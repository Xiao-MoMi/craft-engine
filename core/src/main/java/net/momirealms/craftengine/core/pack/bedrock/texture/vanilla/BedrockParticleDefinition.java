package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.util.List;
import java.util.Map;

record BedrockParticleDefinition(String file,
                                 String formatVersion,
                                 String identifier,
                                 String material,
                                 Map<String, Object> components,
                                 Map<String, Object> events,
                                 Map<String, Object> curves) {

    JsonObject create(String texture, BedrockParticleRule.Selection selection, int count, String frame) {
        JsonObject output = new JsonObject();
        output.addProperty("format_version", this.formatVersion);
        JsonObject effect = new JsonObject();
        output.add("particle_effect", effect);
        effect.add("description", GsonHelper.get().toJsonTree(Map.of(
                "identifier", this.identifier,
                "basic_render_parameters", Map.of("material", this.material, "texture", texture)
        )));
        // 每次创建独立输出对象，修改 UV 不会污染共享的行为常量。
        JsonObject components = GsonHelper.get().toJsonTree(this.components).getAsJsonObject();
        effect.add("components", components);
        if (!this.events.isEmpty()) {
            effect.add("events", GsonHelper.get().toJsonTree(this.events));
        }
        if (!this.curves.isEmpty()) {
            effect.add("curves", GsonHelper.get().toJsonTree(this.curves));
        }
        JsonObject uv = new JsonObject();
        uv.addProperty("texture_width", 1);
        uv.addProperty("texture_height", count);
        if (selection == BedrockParticleRule.Selection.QUARTER) {
            uv.add("uv", GsonHelper.get().toJsonTree(List.of("variable.particle_random_1 * 0.75", "(" + frame + ") + variable.particle_random_2 * 0.75")));
            uv.add("uv_size", GsonHelper.get().toJsonTree(List.of(0.25, 0.25)));
        } else {
            uv.add("uv", GsonHelper.get().toJsonTree(List.of(0, frame)));
            uv.add("uv_size", GsonHelper.get().toJsonTree(List.of(1, 1)));
        }
        components.getAsJsonObject("minecraft:particle_appearance_billboard").add("uv", uv);
        return output;
    }
}
