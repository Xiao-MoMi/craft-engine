package net.momirealms.craftengine.core.pack.bedrock;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BedrockPlayerAnimations {
    private BedrockPlayerAnimations() {
    }

    static void write(Path root, List<BedrockPackMappings.Item> items) throws IOException {
        for (BedrockPackMappings.Item item : items) {
            if (item.crossbowCharge() != null) {
                Path target = root.resolve("animation_controllers/ce_player.json");
                Files.createDirectories(target.getParent());
                try (InputStream source = BedrockPlayerAnimations.class.getResourceAsStream("/bedrock/player.animation_controllers.json")) {
                    if (source == null) {
                        throw new IOException("Missing bundled Bedrock player animation controllers");
                    }
                    Files.copy(source, target);
                }
                return;
            }
        }
    }
}
