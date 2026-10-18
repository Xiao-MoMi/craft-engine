package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockSpearWriter {
    private final BedrockStateItemWriter writer;
    private final Path root;

    public BedrockSpearWriter(JavaModelResolver resolver, BedrockTextureAtlas.TextureSource textures, BedrockItemIcons icons,
                              BedrockBlockItems blocks, Path root) {
        this.writer = new BedrockStateItemWriter(resolver, textures, icons, blocks, root);
        this.root = root;
    }

    public Result write(JavaDisplayContextModel spear, List<BedrockItemSource> items) throws IOException {
        Key owner = items.getFirst().id();
        BaseItemModel inventory = spear.model(DisplayPosition.GUI);
        if (!inventory.equals(spear.model(DisplayPosition.GROUND)) || !inventory.equals(spear.model(DisplayPosition.FIXED))) {
            throw new IOException("Spear gui, ground and fixed currently require the same model");
        }
        Map<BaseItemModel, String> states = new LinkedHashMap<>();
        for (BedrockItemDisplay.View view : BedrockItemDisplay.VIEWS) {
            states.merge(spear.model(view.position), '(' + view.condition + ')', (first, second) -> first + " || " + second);
        }
        states.replaceAll((leaf, condition) -> '(' + condition + ')');
        BedrockStateItemWriter.Result result = this.writer.write(owner, "spear", states, inventory, new JsonArray(), false, BedrockWeaponDisplay.SPEAR);
        String name = HashUtils.sha1(owner.asString().getBytes(StandardCharsets.UTF_8));
        JsonObject description = result.attachable().getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
        this.addHitAnimation(description, name, states.size());
        for (BedrockItemSource item : items) {
            this.writer.writeAttachable(result.attachable(), item.id());
        }
        return new Result(result.icon(), result.displayBlock(), result.models());
    }

    private void addHitAnimation(JsonObject description, String name, int states) throws IOException {
        String animation = "animation.ce_spear_" + name + ".hit";
        String controller = "controller.animation.ce_spear_" + name + ".hit";
        description.getAsJsonObject("animations").addProperty("ce_spear_hit", animation);
        description.getAsJsonObject("animations").addProperty("ce_spear_hit_controller", controller);
        JsonObject enabled = new JsonObject();
        enabled.addProperty("ce_spear_hit_controller", "context.item_slot == 'main_hand'");
        description.getAsJsonObject("scripts").getAsJsonArray("animate").add(enabled);
        JsonObject hit = new JsonObject();
        hit.addProperty("animation_length", 0.5);
        JsonObject bones = new JsonObject();
        JsonObject translation = new JsonObject();
        translation.add("0.0", BedrockWeaponDisplay.expressions("0", "0", "0"));
        translation.add("0.05", BedrockWeaponDisplay.expressions("0", "0", "1.5"));
        translation.add("0.1", BedrockWeaponDisplay.expressions("0", "0", "7.5"));
        translation.add("0.2", BedrockWeaponDisplay.expressions("0", "0", "6"));
        translation.add("0.5", BedrockWeaponDisplay.expressions("0", "0", "0"));
        JsonObject pose = new JsonObject();
        pose.add("position", translation);
        for (int index = 0; index < states; index++) {
            String prefix = "ce_spear_" + index + '_';
            // Include animated texture rigs: they must recoil with the static faces.
            for (String geometry : description.getAsJsonObject("geometry").keySet()) {
                if (geometry.startsWith(prefix)) {
                    String bonePrefix = geometry.equals(prefix + "default") ? prefix : geometry + '_';
                    bones.add(bonePrefix + BedrockWeaponDisplay.POSE, pose);
                }
            }
        }
        hit.add("bones", bones);
        JsonObject animations = new JsonObject();
        animations.add(animation, hit);
        this.write("animations/ce_spear_" + name + "_hit.json", "animations", animations);
        JsonObject stateMachine = GsonHelper.get().fromJson("""
                {"initial_state":"default","states":{
                  "default":{"transitions":[{"hit":"context.owning_entity->query.ticks_since_last_kinetic_weapon_hit >= 0 && context.owning_entity->query.ticks_since_last_kinetic_weapon_hit <= 5"}]},
                  "hit":{"animations":["ce_spear_hit"],"transitions":[{"default":"query.all_animations_finished"}]}
                }}
                """, JsonObject.class);
        JsonObject controllers = new JsonObject();
        controllers.add(controller, stateMachine);
        this.write("animation_controllers/ce_spear_" + name + ".json", "animation_controllers", controllers);
    }

    private void write(String path, String field, JsonObject value) throws IOException {
        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.10.0");
        file.add(field, value);
        Path target = this.root.resolve(path);
        Files.createDirectories(target.getParent());
        GsonHelper.writeJsonFile(file, target);
    }

    public record Result(String icon, BedrockPackMappings.DisplayBlock displayBlock, int models) {
    }
}
