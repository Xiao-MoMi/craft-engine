package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.pack.allocator.VisualBlockStateAllocator;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class DebugCleanCacheCommand extends BukkitCommandFeature<CommandSender> {

    public DebugCleanCacheCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .optional("type", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(List.of(Suggestion.suggestion("custom-model-data"), Suggestion.suggestion("custom-block-states"), Suggestion.suggestion("visual-block-states"), Suggestion.suggestion("font"), Suggestion.suggestion("all")));
                    }
                }))
                .handler(context -> {
                    if (this.plugin().isReloading()) {
                        context.sender().sendMessage("The plugin is reloading. Please wait until the process is complete.");
                        return;
                    }
                    String type = context.getOrDefault("type", "all");
                    switch (type) {
                        case "custom-model-data" -> handleCustomModelData(context);
                        case "font", "images" -> handleFont(context);
                        case "custom-block-states" -> handleCustomBlockState(context);
                        case "visual-block-states" -> handleVisualBlockState(context);
                        case "all" -> {
                            handleCustomModelData(context);
                            handleFont(context);
                            handleCustomBlockState(context);
                            handleVisualBlockState(context);
                        }
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_clean_cache";
    }

    private void handleVisualBlockState(CommandContext<CommandSender> context) {
        BukkitBlockManager instance = BukkitBlockManager.instance();
        Set<BlockStateWrapper> ids = new HashSet<>();
        for (CustomBlock customBlock : instance.loadedBlocks().values()) {
            for (ImmutableBlockState state : customBlock.variantProvider().states()) {
                ids.add(state.visualBlockState());
            }
        }
        VisualBlockStateAllocator visualBlockStateAllocator = instance.blockParser().visualBlockStateAllocator();
        List<String> removed = visualBlockStateAllocator.cleanupUnusedIds(i -> !ids.contains(i));
        try {
            visualBlockStateAllocator.saveToCache();
        } catch (IOException e) {
            this.plugin().logger().warn("Error while saving visual block states allocation", e);
        }
        for (String id : removed) {
            this.plugin().logger().info("Cleaned unsued block appearance: " + id);
        }
        context.sender().sendMessage("Cleaned " + removed.size() + " unused block state appearances");
    }

    private void handleCustomBlockState(CommandContext<CommandSender> context) {
        BukkitBlockManager instance = BukkitBlockManager.instance();
        Set<String> ids = new HashSet<>();
        for (CustomBlock customBlock : instance.loadedBlocks().values()) {
            for (ImmutableBlockState state : customBlock.variantProvider().states()) {
                ids.add(state.toString());
            }
        }
        IdAllocator idAllocator = instance.blockParser().internalIdAllocator();
        List<String> removed = idAllocator.cleanupUnusedIds(i -> !ids.contains(i));
        try {
            idAllocator.saveToCache();
        } catch (IOException e) {
            this.plugin().logger().warn("Error while saving custom block states allocation", e);
        }
        for (String id : removed) {
            this.plugin().logger().info("Cleaned unsued block state: " + id);
        }
        context.sender().sendMessage("Cleaned " + removed.size() + " unused custom block states");
    }

    private void handleFont(CommandContext<CommandSender> context) {
        BukkitFontManager instance = this.plugin().fontManager();
        Map<Key, Set<String>> idsMap = new HashMap<>();
        for (BitmapImage image : instance.loadedBitmapImages().values()) {
            Set<String> ids = idsMap.computeIfAbsent(image.font(), k -> new HashSet<>());
            String id = image.id().toString();
            ids.add(id);
            for (int i = 0; i < image.rows(); i++) {
                for (int j = 0; j < image.columns(); j++) {
                    String imageArgs = id + ":" + i + ":" + j;
                    ids.add(imageArgs);
                }
            }
        }
        int total = 0;
        for (Map.Entry<Key, IdAllocator> entry : getAllCachedFont().entrySet()) {
            Key font = entry.getKey();
            Set<String> ids = idsMap.getOrDefault(font, Collections.emptySet());
            List<String> removed = entry.getValue().cleanupUnusedIds(i -> !ids.contains(i));
            try {
                entry.getValue().saveToCache();
            } catch (IOException e) {
                this.plugin().logger().warn("Error while saving codepoint allocation for font " + font.asString(), e);
                return;
            }
            for (String id : removed) {
                this.plugin().logger().info("Cleaned unsued image: " + id);
            }
            total += removed.size();
        }
        context.sender().sendMessage("Cleaned " + total + " unused codepoints");
    }

    private void handleCustomModelData(CommandContext<CommandSender> context) {
        BukkitItemManager instance = BukkitItemManager.instance();
        Map<Key, Set<String>> idsMap = new HashMap<>();
        for (CustomItem<ItemStack> item : instance.loadedItems().values()) {
            Set<String> ids = idsMap.computeIfAbsent(item.clientBoundMaterial(), k -> new HashSet<>());
            ids.add(item.id().asString());
        }
        int total = 0;
        for (Map.Entry<Key, IdAllocator> entry : getAllCachedCustomModelData().entrySet()) {
            Set<String> ids = idsMap.getOrDefault(entry.getKey(), Collections.emptySet());
            List<String> removed = entry.getValue().cleanupUnusedIds(i -> !ids.contains(i));
            total += removed.size();
            try {
                entry.getValue().saveToCache();
            } catch (IOException e) {
                this.plugin().logger().warn("Error while saving custom model data allocation for material " + entry.getKey().asString(), e);
                return;
            }
            for (String id : removed) {
                this.plugin().logger().info("Cleaned unsued item: " + id);
            }
        }
        context.sender().sendMessage("Cleaned " + total + " unused custom model data");
    }

    public Map<Key, IdAllocator> getAllCachedCustomModelData() {
        Path cacheDir = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("custom-model-data");
        if (!Files.exists(cacheDir)) {
            return Map.of();
        }

        Map<Key, IdAllocator> idAllocators = new HashMap<>();
        try (Stream<Path> files = Files.list(cacheDir)) {
            files.filter(this::isJsonFile)
                    .forEach(file -> processIdAllocatorFile("minecraft", file, idAllocators));

        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to process: " + cacheDir.getFileName(), e);
        }

        return idAllocators;
    }

    public Map<Key, IdAllocator> getAllCachedFont() {
        Path cacheDir = CraftEngine.instance().dataFolderPath().resolve("cache").resolve("font");

        try {
            List<Path> namespaces = FileUtils.collectNamespaces(cacheDir);
            Map<Key, IdAllocator> idAllocators = new HashMap<>();

            for (Path namespace : namespaces) {
                processNamespace(namespace, idAllocators);
            }
            return idAllocators;

        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to load cached id allocators from: " + cacheDir, e);
            return Collections.emptyMap();
        }
    }

    private void processNamespace(Path namespace, Map<Key, IdAllocator> idAllocators) {
        if (!Files.isDirectory(namespace)) {
            return;
        }

        try (Stream<Path> files = Files.list(namespace)) {
            files.filter(this::isJsonFile)
                    .forEach(file -> processIdAllocatorFile(namespace.getFileName().toString(), file, idAllocators));

        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to process namespace: " + namespace.getFileName(), e);
        }
    }

    private boolean isJsonFile(Path file) {
        return Files.isRegularFile(file) && file.getFileName().toString().endsWith(".json");
    }

    private void processIdAllocatorFile(String namespaceName, Path file, Map<Key, IdAllocator> idAllocators) {
        try {
            String fileName = FileUtils.pathWithoutExtension(file.getFileName().toString());

            Key font = Key.of(namespaceName, fileName);
            IdAllocator allocator = new IdAllocator(file);
            allocator.loadFromCache();

            idAllocators.put(font, allocator);

        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load id allocator from: " + file, e);
        }
    }
}
