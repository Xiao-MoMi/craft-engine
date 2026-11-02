package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.PLAYER_ANIMATION_CONTROLLER;

final class BedrockPlayerAnimationController {
    private final Path path;
    private JsonObject file;

    BedrockPlayerAnimationController(Path root) {
        this.path = root.resolve(PLAYER_ANIMATION_CONTROLLER);
    }

    JsonObject state(String name) throws IOException {
        if (this.file == null) {
            this.file = Files.exists(this.path) ? GsonHelper.readJsonObjectFromFile(this.path)
                    : GsonHelper.readJsonFromResource("/bedrock/player/root.animation_controllers.json").getAsJsonObject();
        }
        return this.file.getAsJsonObject("animation_controllers").getAsJsonObject("controller.animation.player.root")
                .getAsJsonObject("states").getAsJsonObject(name);
    }

    JsonArray animations(String state) throws IOException {
        return this.state(state).getAsJsonArray("animations");
    }

    void write() throws IOException {
        if (this.file != null) {
            Files.createDirectories(this.path.getParent());
            GsonHelper.writeJsonFile(this.file, this.path);
        }
    }
}
