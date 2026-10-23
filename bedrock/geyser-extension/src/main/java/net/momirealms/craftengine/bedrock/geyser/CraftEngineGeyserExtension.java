package net.momirealms.craftengine.bedrock.geyser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockRegistrationAdapter;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationAdapter;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserSelectedItemHandler;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserSpearComponents;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class CraftEngineGeyserExtension implements Extension {
    private final Map<String, String> displayBlocks = new HashMap<>();
    private final List<GeyserSelectedItemHandler.Variants> selectedItems = new ArrayList<>();
    private final Map<String, JsonObject> spears = new HashMap<>();

    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        this.displayBlocks.clear();
        for (Path path : this.packFiles()) {
            this.registerBlocks(path, event);
        }
        this.logger().info("Registered " + this.displayBlocks.size() + " CraftEngine display blocks.");
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        this.selectedItems.clear();
        this.spears.clear();
        for (Path path : this.packFiles()) {
            this.registerItems(path, event);
        }
    }

    private List<Path> packFiles() {
        Path directory = this.geyserApi().packDirectory();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.endsWith(".mcpack") || name.endsWith(".zip");
            }).sorted().toList();
        } catch (IOException exception) {
            this.logger().error("Failed to scan Geyser packs directory " + directory, exception);
            return List.of();
        }
    }

    private void registerBlocks(Path path, GeyserDefineCustomBlocksEvent event) {
        try {
            JsonObject plan = GeyserRegistrationPlanReader.readPack(path, RegistrationPlanKind.BLOCK);
            if (plan == null) {
                return;
            }
            for (JsonElement value : plan.getAsJsonArray(ENTRIES)) {
                JsonObject display = value.getAsJsonObject();
                String name = display.get(NAME).getAsString();
                if (!this.displayBlocks.containsKey(name)) {
                    try {
                        this.displayBlocks.put(name, GeyserBlockRegistrationAdapter.registerBlock(display, event));
                    } catch (RuntimeException exception) {
                        this.logger().error("Failed to register display block " + name + " from " + path, exception);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            this.logger().error("Failed to read display blocks from " + path, exception);
        }
    }

    private void registerItems(Path packPath, GeyserDefineCustomItemsEvent event) {
        GeyserItemRegistrationAdapter adapter;
        JsonArray entries;
        try {
            // 没有找到注册计划，跳过
            JsonObject plan = GeyserRegistrationPlanReader.readPack(packPath, RegistrationPlanKind.ITEM);
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
        int failed = 0;
        int skipped = 0;
        for (JsonElement value : entries) {
            JsonObject entry = value.getAsJsonObject();
            try {
                GeyserItemRegistrationAdapter.ConvertedItem converted = adapter.convert(entry, this.displayBlocks);
                for (CustomItemDefinition definition : converted.definitions()) {
                    event.register(Identifier.of(entry.get(JAVA_ITEM).getAsString()), definition);
                    registered++;
                }
                if (converted.selected() != null) {
                    this.selectedItems.add(converted.selected());
                }
                String javaItem = entry.get(JAVA_ITEM).getAsString();
                if (javaItem.startsWith("minecraft:") && javaItem.endsWith("_spear")) {
                    for (JsonElement branch : entry.getAsJsonArray(VARIANTS)) {
                        JsonObject variant = branch.getAsJsonObject();
                        if (variant.getAsJsonObject(RESOURCES).has(ATTACHABLE_IDENTIFIER)) {
                            this.spears.put(variant.get(BEDROCK_IDENTIFIER).getAsString(), entry);
                        }
                    }
                }
            } catch (UnsupportedOperationException exception) {
                skipped++;
                this.logger().warning("Skipped custom item " + entry.get(BEDROCK_IDENTIFIER) + " [model="
                        + entry.get(MODEL) + ", pack=" + packPath + "]: " + exception.getMessage());
            } catch (RuntimeException exception) {
                failed++;
                this.logger().error("Failed to register custom item " + entry.get(BEDROCK_IDENTIFIER)
                        + " [java=" + entry.get(JAVA_ITEM) + ", model=" + entry.get(MODEL)
                        + ", pack=" + packPath + "]", exception);
            }
        }
        this.logger().info("Registered " + registered + " custom item variants from " + packPath
                + "; skipped=" + skipped + ", failed=" + failed + ".");
    }

    @Subscribe
    public void onSessionInitialize(SessionInitializeEvent event) {
        if (!this.spears.isEmpty()) {
            GeyserSpearComponents.install((GeyserSession) event.connection(), this.spears);
        }
    }

    @Subscribe
    public void onSessionJoin(SessionJoinEvent event) {
        if (!this.selectedItems.isEmpty()) {
            try {
                GeyserSelectedItemHandler.install((GeyserSession) event.connection(), this.selectedItems);
            } catch (RuntimeException exception) {
                this.logger().error("Failed to install Geyser selected-item adapter", exception);
            }
        }
    }
}
