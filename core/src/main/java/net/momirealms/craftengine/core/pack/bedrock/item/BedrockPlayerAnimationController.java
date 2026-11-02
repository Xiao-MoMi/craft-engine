package net.momirealms.craftengine.core.pack.bedrock.item;

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

    BedrockPlayerAnimationState state(String name) throws IOException {
        if (this.file == null) {
            this.file = Files.exists(this.path) ? GsonHelper.readJsonObjectFromFile(this.path)
                    : GsonHelper.readJsonFromResource("/bedrock/player/root.animation_controllers.json").getAsJsonObject();
        }
        JsonObject states = this.file.getAsJsonObject("animation_controllers").getAsJsonObject("controller.animation.player.root")
                .getAsJsonObject("states");
        return new BedrockPlayerAnimationState(states.getAsJsonObject(name));
    }

    void write() throws IOException {
        if (this.file != null) {
            Files.createDirectories(this.path.getParent());
            GsonHelper.writeJsonFile(this.file, this.path);
        }
    }
}
