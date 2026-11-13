package net.momirealms.craftengine.core.pack.bedrock.scoreboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class BedrockSidebarGenerator {
    private BedrockSidebarGenerator() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        write(resources, root, Config.bedrockScoreboard());
    }

    public static void write(JavaResourcePack resources, Path root, BedrockSidebarConfig config) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("enabled", config.enable());
        JsonArray entries = new JsonArray();
        for (BedrockSidebarConfig.Profile profile : config.profiles()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("objective-id", profile.objectiveId());
            entry.addProperty("title-contains", profile.titleContains());
            entry.addProperty("offset-x", profile.offsetX());
            int index = entries.size() + 1;
            entry.addProperty("index", index);
            JsonArray elements = new JsonArray();
            entry.add("elements", elements);
            for (BedrockSidebarConfig.Element element : profile.elements()) {
                JsonObject image = new JsonObject();
                image.addProperty("id", element.id());
                image.addProperty("width", element.width());
                image.addProperty("height", element.height());
                image.addProperty("anchor", element.anchor().name().toLowerCase(Locale.ROOT));
                image.addProperty("layer", element.layer());
                image.addProperty("x", element.x());
                image.addProperty("y", element.y());
                String source = "assets/" + element.texture().namespace() + "/textures/" + element.texture().value() + ".png";
                String destination = "textures/craftengine/scoreboard/" + index + "_" + elements.size();
                Path file = root.resolve(destination + ".png");
                Files.createDirectories(file.getParent());
                try (InputStream input = resources.openResource(source)) {
                    if (input == null) {
                        KnownResourceException error = new KnownResourceException("resource_pack.bedrock.sidebar.texture", element.texturePath(), source);
                        throw new IOException(TranslationManager.message("resource.errors_detail", "1", error.node(), error.getLocalizedMessage()), error);
                    }
                    Files.copy(input, file);
                }
                image.addProperty("texture", destination);
                elements.add(image);
            }
            entries.add(entry);
        }
        data.add("profiles", entries);
        Files.createDirectories(root.resolve(BedrockPackLayout.SCOREBOARD).getParent());
        GsonHelper.writeJsonFile(data, root.resolve(BedrockPackLayout.SCOREBOARD));
        if (config.enable()) {
            new BedrockSidebarWriter(entries).write(root);
        }
    }
}
