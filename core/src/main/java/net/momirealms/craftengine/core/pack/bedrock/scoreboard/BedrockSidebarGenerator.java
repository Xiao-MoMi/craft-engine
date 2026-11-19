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
        write(resources, root, root.resolve(BedrockPackLayout.SCOREBOARD), config);
    }

    public static void write(JavaResourcePack resources, Path root, Path scoreboardData, BedrockSidebarConfig config) throws IOException {
        if (!config.enable()) {
            return;
        }
        JsonArray matches = new JsonArray();
        JsonArray layouts = new JsonArray();
        for (BedrockSidebarConfig.Profile profile : config.profiles()) {
            int index = matches.size() + 1;
            JsonObject match = new JsonObject();
            match.addProperty("index", index);
            if (!profile.objectiveId().isEmpty()) {
                match.addProperty("objective-id", profile.objectiveId());
            }
            if (!profile.titleContains().isEmpty()) {
                match.addProperty("title-contains", profile.titleContains());
            }
            matches.add(match);

            JsonObject layout = new JsonObject();
            layout.addProperty("index", index);
            layout.addProperty("offset-x", profile.offsetX());
            JsonArray elements = new JsonArray();
            layout.add("elements", elements);
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
                String destination = "textures/ce/scoreboard/" + index + "_" + elements.size();
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
            layouts.add(layout);
        }
        Files.createDirectories(scoreboardData.getParent());
        GsonHelper.writeJsonFile(GsonHelper.createObject("profiles", matches), scoreboardData);
        new BedrockSidebarWriter(layouts).write(root);
    }
}
