package net.momirealms.craftengine.bedrock.geyser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationAdapter;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class CraftEngineGeyserExtension implements Extension {
    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        Path directory = this.geyserApi().packDirectory();
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".mcpack") || name.endsWith(".zip")) {
                    this.registerPack(path, event);
                }
            }
        } catch (IOException exception) {
            this.logger().error("Failed to scan Geyser packs directory " + directory, exception);
        }
    }

    private void registerPack(Path packPath, GeyserDefineCustomItemsEvent event) {
        GeyserItemRegistrationAdapter adapter;
        JsonArray entries;
        try {
            // 没有找到注册计划，跳过
            JsonObject plan = GeyserItemRegistrationAdapter.readPack(packPath);
            if (plan == null) {
                return;
            }
            adapter = new GeyserItemRegistrationAdapter(plan.get(SOURCE_VERSION).getAsString());
            entries = plan.getAsJsonArray(ENTRIES);
        } catch (IOException | RuntimeException exception) {
            this.logger().error("Failed to read item registration plan from " + packPath, exception);
            return;
        }
        int registered = 0;
        for (JsonElement value : entries) {
            JsonObject entry = value.getAsJsonObject();
            try {
                event.register(Identifier.of(entry.get(JAVA_ITEM).getAsString()), adapter.convert(entry));
                registered++;
            } catch (RuntimeException exception) {
                this.logger().error(
                        "Failed to register custom item " + entry.get(BEDROCK_IDENTIFIER)
                                + " [java=" + entry.get(JAVA_ITEM)
                                + ", model=" + entry.get(MODEL)
                                + ", file=assets/" + entry.get(MODEL).getAsString().replace(":", "/items/") + ".json"
                                + ", pack=" + packPath + "]", exception
                );
            }
        }
        int failed = entries.size() - registered;
        String summary = "Registered " + registered + "/" + entries.size() + " custom items from " + packPath + "; failed=" + failed + ".";
        if (failed > 0) {
            this.logger().warning(summary);
        } else {
            this.logger().info(summary);
        }
    }

    @Subscribe
    public void onInitialized(GeyserPostInitializeEvent event) {
    }

    @Subscribe
    public void onReloaded(GeyserPostReloadEvent event) {
    }
}
