package net.momirealms.craftengine.bukkit.plugin.command.debug;

import com.google.gson.*;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;

public final class DebugGenerateInternalAssetsCommand extends BukkitCommandFeature<CommandSender> {

    public DebugGenerateInternalAssetsCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("path", StringParser.stringComponent(StringParser.StringMode.GREEDY_FLAG_YIELDING))
                .handler(context -> {
                    // 这里指向的完整的minecraft原版资源包文件夹路径
                    String pathName = context.get("path");
                    Path resourcePackPath = this.plugin().dataFolderPath().resolve(pathName.replace('\\', '/'));
                    if (!Files.exists(resourcePackPath)) {
                        context.sender().sendMessage("Could not find path: " + resourcePackPath);
                        return;
                    }
                    Path assetsPath = resourcePackPath.resolve("assets");
                    Path internalPath = resourcePackPath.resolve("internal");
                    if (!Files.exists(assetsPath)) {
                        context.sender().sendMessage("Could not find path: " + assetsPath);
                        return;
                    }
                    Path minecraftNamespacePath = assetsPath.resolve("minecraft");
                    if (!Files.exists(minecraftNamespacePath)) {
                        context.sender().sendMessage("Could not find path: " + minecraftNamespacePath);
                        return;
                    }

                    // 复制atlas
                    {
                        for (String fileName : List.of("blocks.json", "items.json")) {
                            Path atlasPath = minecraftNamespacePath.resolve("atlases").resolve(fileName);
                            Path assetsAtlasPath = internalPath.resolve("atlases").resolve(fileName);
                            try {
                                Files.createDirectories(assetsAtlasPath.getParent());
                                Files.copy(atlasPath, assetsAtlasPath, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                plugin().logger().warn("Failed to copy atlas file", e);
                            }
                        }
                    }

                    // 复制sounds
                    {
                        Path soundPath = minecraftNamespacePath.resolve("sounds.json");
                        Path targetSoundPath = internalPath.resolve("sounds.json");
                        try {
                            Files.createDirectories(targetSoundPath.getParent());
                            Files.copy(soundPath, targetSoundPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            plugin().logger().warn("Failed to create internal sounds file", e);
                        }
                    }

                    // 复制items
                    {
                        Path allPath = minecraftNamespacePath.resolve("items").resolve("_all.json");
                        if (Files.exists(allPath)) {
                            try {
                                Path targetAllPath = internalPath.resolve("items").resolve("_all.json");
                                Files.createDirectories(targetAllPath.getParent());
                                Files.copy(allPath, targetAllPath, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                plugin().logger().warn("Failed to create internal items file", e);
                            }
                        }
                    }

                    // 复制models
                    {
                        for (String name : List.of("block", "item")) {
                            Path allPath = minecraftNamespacePath.resolve("models").resolve(name).resolve("_all.json");
                            Path targetAllPath = internalPath.resolve("models").resolve(name).resolve("_all.json");
                            try {
                                Files.createDirectories(targetAllPath.getParent());
                                Files.copy(allPath, targetAllPath, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                plugin().logger().warn("Failed to create internal models file", e);
                            }
                        }
                    }

                    // 收集textures
                    JsonArray allTextures = new JsonArray();
                    collectListJson(minecraftNamespacePath.resolve("textures"), "", allTextures::add);
                    try {
                        Path resolve = internalPath.resolve("textures/processed.json");
                        Files.createDirectories(resolve.getParent());
                        GsonHelper.writeJsonFile(allTextures, resolve);
                    } catch (IOException e) {
                        plugin().logger().warn("Failed to collect textures", e);
                    }

                    // 收集sounds
                    JsonArray allSounds = new JsonArray();
                    collectListJson(minecraftNamespacePath.resolve("sounds"), "", allSounds::add);
                    try {
                        Path resolve = internalPath.resolve("sounds/processed.json");
                        Files.createDirectories(resolve.getParent());
                        GsonHelper.writeJsonFile(allSounds, resolve);
                    } catch (IOException e) {
                        plugin().logger().warn("Failed to collect sounds", e);
                    }

                    // 收集lang
                    JsonArray allLang = new JsonArray();
                    try {
                        Path allPath = minecraftNamespacePath.resolve("lang/_list.json");
                        JsonObject langJson = GsonHelper.readJsonFromFile(allPath).getAsJsonObject();
                        for (JsonElement file : langJson.getAsJsonArray("files")) {
                            String string = file.getAsString();
                            if ("deprecated.json".equals(string)) continue;
                            allLang.add(string.substring(0, string.length() - ".json".length()));
                        }
                        Path resolve = internalPath.resolve("lang/processed.json");
                        Files.createDirectories(resolve.getParent());
                        GsonHelper.writeJsonFile(allLang, resolve);
                    } catch (Exception e) {
                        plugin().logger().warn("Failed to collect lang", e);
                    }

                    context.sender().sendMessage("Done");
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_generate_internal_assets";
    }

    private void collectListJson(Path folder, String prefix, Consumer<String> callback) {
        try (InputStream inputStream = Files.newInputStream(folder.resolve("_list.json"))) {
            String s = prefix.isEmpty() ? "" : (prefix + "/");
            JsonObject listJson = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray fileList = listJson.getAsJsonArray("files");
            for (JsonElement element : fileList) {
                if (element instanceof JsonPrimitive primitive) {
                    callback.accept(s + FileUtils.pathWithoutExtension(primitive.getAsString()));
                }
            }
            JsonArray directoryList = listJson.getAsJsonArray("directories");
            for (JsonElement element : directoryList) {
                if (element instanceof JsonPrimitive primitive) {
                    collectListJson(folder.resolve(primitive.getAsString()), s + primitive.getAsString(), callback);
                }
            }
        } catch (IOException e) {
            this.plugin().logger().warn("Failed to load _list.json" + folder, e);
        }
    }
}
