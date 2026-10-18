package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.PngOptimizer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class BedrockVanillaTextures {

    private BedrockVanillaTextures() {
    }

    static void write(JavaResourcePack pack, Path root) throws IOException {
        JsonObject mappings;
        try (InputStream input = BedrockVanillaTextures.class.getResourceAsStream("/bedrock/vanilla-textures.json")) {
            if (input == null) {
                throw new IOException("Missing bundled Bedrock vanilla texture mappings");
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                mappings = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("textures");
            }
        }
        for (Map.Entry<String, JsonElement> mapping : mappings.entrySet()) {
            String path = "assets/minecraft/textures/" + mapping.getKey() + ".png";
            ByteArrayOutputStream encoded;
            try (InputStream input = pack.openCustomResource(path)) {
                if (input == null) {
                    continue;
                }
                JsonObject metadata = pack.readResourceMetadata(path);
                if (metadata != null && metadata.has("animation")) {
                    continue;
                }
                BufferedImage image = ClientPngReader.read(input);
                if (image == null) {
                    throw new IOException("Invalid PNG");
                }
                if (image.getWidth() != image.getHeight()) {
                    continue;
                }
                encoded = new ByteArrayOutputStream();
                new PngOptimizer(image, 0).write(encoded);
            } catch (IOException | IllegalArgumentException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + path + ": " + exception.getMessage());
                continue;
            }
            for (JsonElement destination : mapping.getValue().getAsJsonArray()) {
                Path target = root.resolve(destination.getAsString());
                Files.createDirectories(target.getParent());
                try (OutputStream output = Files.newOutputStream(target)) {
                    encoded.writeTo(output);
                }
            }
        }
    }
}
