package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.util.NBTUtils;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.DoubleTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DebugOptimizeFurnitureStructureCommand extends BukkitCommandFeature<CommandSender> {

    public DebugOptimizeFurnitureStructureCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("world", WorldParser.worldParser())
                .required("file", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        World world = context.get("world");
                        Path generated = world.getWorldFolder().toPath().resolve("generated");
                        if (!Files.exists(generated)) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                        try {
                            return CompletableFuture.completedFuture(findStructures(generated).stream().map(Suggestion::suggestion).collect(Collectors.toList()));
                        } catch (IOException e) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                    }
                }))
                .optional("y-offset", DoubleParser.doubleParser())
                .handler(context -> {
                    World world = context.get("world");
                    NamespacedKey file = context.get("file");
                    double offset = (double) context.optional("y-offset").orElse(0d);
                    Path filePath = world.getWorldFolder().toPath().resolve("generated").resolve(file.getNamespace()).resolve("structures").resolve(file.getKey() + ".nbt");
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    if (!Files.exists(filePath)) {
                        sender.sendMessage(Component.text("File not found", NamedTextColor.RED));
                        return;
                    }
                    try (InputStream is = Files.newInputStream(filePath)) {
                        CompoundTag structureTag = NBTUtils.readCompressed(is);
                        ListTag entitiesTag = structureTag.getList("entities");
                        if (entitiesTag == null) {
                            sender.sendMessage(Component.text("Entities not found", NamedTextColor.RED));
                            return;
                        }
                        int count = 0;
                        ListTag toSave = new ListTag();
                        for (Tag entityTag0 : entitiesTag) {
                            CompoundTag entityTag = (CompoundTag) entityTag0;
                            CompoundTag entityNBTTag = (CompoundTag) entityTag.get("nbt");
                            if (entityNBTTag != null) {
                                CompoundTag bukkitValuesTag = (CompoundTag) entityNBTTag.get("BukkitValues");
                                if (bukkitValuesTag != null) {
                                    // 不保存碰撞实体
                                    if (bukkitValuesTag.containsKey("craftengine:collision")) {
                                        count++;
                                        continue;
                                    }
                                    if (bukkitValuesTag.containsKey("craftengine:furniture_id")) {
                                        if (offset != 0) {
                                            ListTag pos = (ListTag) entityTag.get("pos");
                                            double previousY = pos.getDouble(1);
                                            pos.set(1, new DoubleTag(previousY + offset));
                                            count++;
                                        }
                                    }
                                }
                            }
                            toSave.add(entityTag);
                        }
                        if (count == 0) {
                            sender.sendMessage(Component.text("Nothing changed", NamedTextColor.WHITE));
                        } else {
                            structureTag.put("entities", toSave);
                            try (OutputStream os = Files.newOutputStream(filePath)) {
                                NBTUtils.writeCompressed(structureTag, os);
                            } catch (IOException e) {
                                sender.sendMessage(Component.text("Internal error", NamedTextColor.RED));
                                plugin().logger().error("Cannot write structure NBT file", e);
                                return;
                            }
                            sender.sendMessage(Component.text(count + " entities modified", NamedTextColor.WHITE));
                        }
                    } catch (IOException e) {
                        sender.sendMessage(Component.text("Internal error", NamedTextColor.RED));
                        plugin().logger().error("Cannot read structure NBT file", e);
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_optimize_furniture_structure";
    }

    public static List<String> findStructures(Path startPath) throws IOException {
        // 并行遍历文件树，使用自定义的BiPredicate进行过滤
        try (Stream<Path> stream = Files.walk(startPath, FileVisitOption.FOLLOW_LINKS)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.endsWith(".nbt") &&
                                filename.length() > 4;  // 确保有文件名（不只是.nbt）
                    })
                    .map(path -> {
                        try {
                            return extractPair(startPath, path);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    private static String extractPair(Path basePath, Path nbtFile) {
        try {
            Path relativePath = basePath.relativize(nbtFile);
            List<String> parts = new ArrayList<>();
            for (Path part : relativePath) {
                parts.add(part.toString());
            }
            // 检查是否符合 xxx/structures/xxx.nbt 模式
            if (parts.size() >= 3 && "structures".equals(parts.get(parts.size() - 2))) {
                String firstXxx = parts.get(parts.size() - 3);
                String fileName = parts.getLast();
                if (fileName.endsWith(".nbt")) {
                    String secondXxx = fileName.substring(0, fileName.length() - 4);
                    return firstXxx + ":" + secondXxx;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
