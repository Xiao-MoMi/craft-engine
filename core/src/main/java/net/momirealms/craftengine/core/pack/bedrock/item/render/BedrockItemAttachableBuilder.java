package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.*;

final class BedrockItemAttachableBuilder {
    static final String IN_HAND = "context.item_slot == 'main_hand' || context.item_slot == 'off_hand'";

    private final Key id;
    private final String resource;
    final String prefix;
    private final JsonObject textures = new JsonObject();
    private final JsonObject geometryNames = new JsonObject();
    private final JsonArray geometries = new JsonArray();
    final JsonObject animations = new JsonObject();
    final JsonObject animationNames = new JsonObject();
    final JsonArray animate = new JsonArray();
    final JsonArray controllers = new JsonArray();

    BedrockItemAttachableBuilder(Key id) {
        this.id = id;
        this.resource = id.namespace() + '/' + id.value();
        this.prefix = ResourcePackUtils.bedrockResourceIdentifier(id);
        this.textures.addProperty("enchanted", "textures/misc/enchanted_item_glint");
    }

    void addGeometry(String alias, String geometryId, String texture, JsonObject geometry) {
        this.geometryNames.addProperty(alias, geometryId);
        this.textures.addProperty(alias, texture);
        this.geometries.add(geometry);
    }

    void showInHand(String controller) {
        this.controllers.add(GsonHelper.createObject(controller, new JsonPrimitive(IN_HAND)));
    }

    void writeStageController(Path root, JsonArray geometryFrames, JsonArray textureFrames) throws IOException {
        String controller = "controller.render." + this.prefix;
        BedrockItemAttachables.writeController(controller, geometryFrames, textureFrames, root.resolve("render_controllers/" + this.resource + ".render_controllers.json"));
        // 没有匹配阶段时 ce_stage 为 -1，必须关闭控制器，避免数组取到错误模型。
        this.controllers.add(GsonHelper.createObject(controller, new JsonPrimitive("(" + IN_HAND + ") && variable.ce_stage >= 0")));
    }

    void write(Path root, @Nullable JsonArray preAnimation) throws IOException {
        ResourcePackUtils.writeBedrockJson(GEOMETRY, "minecraft:geometry", this.geometries,
                root.resolve("models/entity/" + this.resource + ".geo.json"));
        // 仅引用原版动画的弩、矛没有本地动画定义，无需额外输出空文件。
        if (!this.animations.isEmpty()) {
            ResourcePackUtils.writeBedrockJson(ANIMATION, "animations", this.animations, root.resolve("animations/" + this.resource + ".animation.json"));
        }

        JsonObject description = new JsonObject();
        description.addProperty("identifier", this.id.asString());
        JsonObject materials = new JsonObject();
        materials.addProperty("default", preAnimation == null ? "entity_alphatest_one_sided" : "entity_alphatest");
        materials.addProperty("enchanted", "entity_alphatest_glint");
        description.add("materials", materials);
        description.add("textures", this.textures);
        description.add("geometry", this.geometryNames);
        description.add("animations", this.animationNames);
        JsonObject scripts = new JsonObject();
        scripts.add("animate", this.animate);
        if (preAnimation != null) {
            scripts.add("pre_animation", preAnimation);
        }
        description.add("scripts", scripts);
        description.add("render_controllers", this.controllers);
        ResourcePackUtils.writeBedrockJson(ATTACHABLE, "minecraft:attachable", GsonHelper.createObject("description", description), root.resolve("attachables/" + this.resource + ".attachable.json"));
    }
}
