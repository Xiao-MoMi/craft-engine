package net.momirealms.craftengine.bedrock.geyser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockRegistrationAdapter;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserContainerCloseHandler;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserGuiMenus;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationAdapter;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRuntime;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatch;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatcher;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatches;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserAdventureHelper;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserGlyphs;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserTranslations;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class CraftEngineGeyserExtension implements Extension {
    private final Map<String, String> displayBlocks = new HashMap<>();
    private final Map<String, CustomItemDefinition> contextualItems = new HashMap<>();
    private Map<String, GeyserItemComponentPatch> componentPatches = Map.of();
    private ContainerTitleLayout titleLayout;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        GeyserAdventureHelper.disableLegacyFormattingWarning();
        List<Path> packs = this.packFiles();
        GeyserTranslations.install(packs, this.geyserApi().configDirectory(), this.logger());
        this.titleLayout = GeyserGlyphs.install(packs, this.logger());
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        GeyserGuiMenus.install(this.logger(), this.titleLayout);
    }

    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
        this.displayBlocks.clear();
        for (Path path : this.packFiles()) {
            this.registerBlocks(path, event);
        }
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        this.contextualItems.clear();
        Map<String, GeyserItemComponentPatch> componentPatches = new HashMap<>();
        for (Path path : this.packFiles()) {
            this.registerItems(path, event, componentPatches);
        }
        componentPatches.replaceAll((identifier, patch) -> GeyserItemComponentPatches.cached(patch));
        this.componentPatches = Map.copyOf(componentPatches);
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

    private void registerItems(Path packPath, GeyserDefineCustomItemsEvent event, Map<String, GeyserItemComponentPatch> componentPatches) {
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
        /*
         * {
         *   "bedrock_identifier": "craftengine:example/rod", // 基础基岩 ID
         *   "item_type": "minecraft:fishing_rod", // 基础 Java 物品
         *   "item_id": "example:rod", // CE 物品 ID
         *   "model": "example:rod", // 匹配的 item_model
         *   "components": {}, // 组件覆写及转换所需有效值，null 表示移除
         *   "variants": [{ // 显示分支
         *     "bedrock_identifier": "craftengine:example/rod_variant_0", // 分支基岩 ID，兼作图标键
         *     "conditions": [{ // 条件全部满足
         *       "property": "fishing_rod/cast", // 抛竿状态
         *       "expected": true // 期望已抛竿
         *     }],
         *     "resources": { // 分支资源
         *       "attachable": true, // 是否已生成手持模型
         *       "generated": true // 是否已生成资源
         *     }
         *   }],
         *   "client_behavior": {"use": {}} // 使用行为，字段省略
         * }
         */
        for (JsonElement value : entries) {
            JsonObject entry = value.getAsJsonObject();
            Identifier identifier = Identifier.of(entry.get(ITEM_TYPE).getAsString());
            try {
                GeyserItemRegistrationAdapter.ConvertedItem converted = adapter.convert(entry, this.displayBlocks);
                for (CustomItemDefinition definition : converted.definitions()) {
                    event.register(identifier, definition);
                    registered++;
                    if (converted.componentPatch() != null) {
                        componentPatches.put(definition.bedrockIdentifier().toString(), converted.componentPatch());
                    }
                }
                if (converted.contextual()) {
                    for (CustomItemDefinition definition : converted.definitions()) {
                        this.contextualItems.put(definition.bedrockIdentifier().toString(), definition);
                    }
                }
            } catch (UnsupportedOperationException exception) {
                skipped++;
                this.logger().warning("Skipped custom item " + entry.get(BEDROCK_IDENTIFIER) + " [model=" + entry.get(MODEL) + ", pack=" + packPath + "]: " + exception.getMessage());
            } catch (RuntimeException exception) {
                failed++;
                this.logger().error("Failed to register custom item " + entry.get(BEDROCK_IDENTIFIER) + " [java=" + entry.get(ITEM_TYPE) + ", model=" + entry.get(MODEL) + ", pack=" + packPath + "]", exception);
            }
        }
        this.logger().info("Registered " + registered + " custom item variants from " + packPath + "; skipped=" + skipped + ", failed=" + failed + ".");
    }

    @Subscribe
    public void onSessionInitialize(SessionInitializeEvent event) {
        GeyserContainerCloseHandler.install((GeyserSession) event.connection());
        if (!this.componentPatches.isEmpty()) {
            GeyserItemComponentPatcher.install((GeyserSession) event.connection(), this.componentPatches);
        }
        if (!this.contextualItems.isEmpty()) {
            GeyserItemRuntime.install((GeyserSession) event.connection(), this.contextualItems);
        }
    }
}
