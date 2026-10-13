package net.momirealms.craftengine.core.pack.validation;

import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

final class ResourceValidation {
    private final ValidationContext context;
    private final ModelReferenceCollector models;

    ResourceValidation(ValidationContext context) {
        this(context, new ModelReferenceCollector(context, Set.of()));
    }

    ResourceValidation(ValidationContext context, ModelReferenceCollector models) {
        this.context = context;
        this.models = models;
    }

    ModelReferences collectAndValidate(Path[] rootPaths, ResourcePackView rpView) {
        MissingResourceReferences glyphToFonts = new MissingResourceReferences(rpView, this.context.resources().textures(), "textures", ".png", 32);
        List<ModelReferences> modelReferences = new ArrayList<>(rootPaths.length);
        MissingResourceReferences textureToEquipments = new MissingResourceReferences(rpView, this.context.resources().textures(), "textures", ".png", 8);
        MissingResourceReferences oggToSoundEvents = new MissingResourceReferences(rpView, this.context.resources().sounds(), "sounds", ".ogg", 4);
        for (Path rootPath : rootPaths) {
            Path assetsPath = rootPath.resolve("assets");
            if (!Files.isDirectory(assetsPath)) {
                continue;
            }
            // 收集全部命名空间
            List<Path> namespaces;
            try {
                namespaces = FileUtils.collectNamespaces(assetsPath);
            } catch (IOException e) {
                this.context.logger().warn("Failed to collect namespaces for " + assetsPath.toAbsolutePath(), e);
                continue;
            }

            ModelReferences rootReferences = this.models.collect(rootPath, namespaces);
            if (!rootReferences.items().isEmpty() || !rootReferences.blocks().isEmpty()) {
                modelReferences.add(rootReferences);
            }

            for (Path namespacePath : namespaces) {
                String namespace = namespacePath.getFileName().toString(); // 命名空间

                // 字体文件夹
                Path fontPath = namespacePath.resolve("font");
                if (Files.isDirectory(fontPath)) {
                    try {
                        Files.walkFileTree(fontPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!FileUtils.isJsonFile(file)) {
                                    return FileVisitResult.CONTINUE;
                                }
                                JsonObject fontJson = ResourceValidation.this.context.readJson(file);
                                if (fontJson == null) {
                                    return FileVisitResult.CONTINUE;
                                }
                                JsonArray providers = fontJson.getAsJsonArray("providers");
                                if (providers != null) {
                                    Key fontName = Key.of(namespace, FileUtils.pathWithoutExtension(file.getFileName().toString()));
                                    for (JsonElement provider : providers) {
                                        if (!(provider instanceof JsonObject providerJO)) {
                                            continue;
                                        }
                                        JsonPrimitive typePrimitive = providerJO.getAsJsonPrimitive("type");
                                        if (typePrimitive == null) {
                                            continue;
                                        }
                                        String type = typePrimitive.getAsString();
                                        if (!type.equals("bitmap")) {
                                            continue;
                                        }
                                        JsonPrimitive filePrimitive = providerJO.getAsJsonPrimitive("file");
                                        if (filePrimitive == null) {
                                            continue;
                                        }
                                        String pngFile = filePrimitive.getAsString();
                                        Key identifier = Key.of(FileUtils.pathWithoutExtension(pngFile));
                                        glyphToFonts.add(identifier, fontName);
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        this.context.logger().warn("Failed to walk through font", e);
                    }
                }

                // 装备
                Path equipmentPath = namespacePath.resolve("equipment");
                if (Files.isDirectory(equipmentPath)) {
                    try {
                        Files.walkFileTree(equipmentPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                            @Override
                            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                                if (!FileUtils.isJsonFile(file)) {
                                    return FileVisitResult.CONTINUE;
                                }
                                JsonObject equipmentJson = ResourceValidation.this.context.readJson(file);
                                if (equipmentJson == null) {
                                    return FileVisitResult.CONTINUE;
                                }
                                String equipmentId = FileUtils.pathWithoutExtension(file.getFileName().toString());
                                if (equipmentJson.get("layers") instanceof JsonObject layersObject) {
                                    for (Map.Entry<String, JsonElement> layerEntry : layersObject.entrySet()) {
                                        String type = layerEntry.getKey();
                                        if (layerEntry.getValue() instanceof JsonArray equipmentLayers) {
                                            for (JsonElement equipmentLayer : equipmentLayers) {
                                                if (equipmentLayer instanceof JsonObject layer && layer.get("texture") instanceof JsonPrimitive layerTexture) {
                                                    Key rawTexture = Key.of(layerTexture.getAsString());
                                                    Key fullPath = Key.of(rawTexture.namespace(), "entity/equipment/" + type + "/" + rawTexture.value());
                                                    textureToEquipments.add(fullPath, Key.of(namespace, equipmentId));
                                                }
                                            }
                                        }
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException | ClassCastException e) {
                        this.context.logger().warn("Failed to walk through equipments", e);
                    }
                }

                // 声音文件
                Path soundsPath = namespacePath.resolve("sounds.json");
                if (Files.exists(soundsPath)) {
                    JsonObject soundsJson = this.context.readJson(soundsPath);
                    if (soundsJson != null) {
                        for (Map.Entry<String, JsonElement> soundEventEntry : soundsJson.entrySet()) {
                            Key soundKey = Key.of(namespace, soundEventEntry.getKey());
                            if (soundEventEntry.getValue() instanceof JsonObject soundEventObj) {
                                JsonArray soundArray = soundEventObj.getAsJsonArray("sounds");
                                if (soundArray != null) {
                                    for (JsonElement sound : soundArray) {
                                        if (sound instanceof JsonPrimitive primitive) {
                                            oggToSoundEvents.add(Key.of(primitive.getAsString()), soundKey);
                                        } else if (sound instanceof JsonObject soundObj && soundObj.get("name") instanceof JsonPrimitive name) {
                                            if (soundObj.get("type") instanceof JsonPrimitive type && type.getAsString().equals("file")) {
                                                oggToSoundEvents.add(Key.of(name.getAsString()), soundKey);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        glyphToFonts.report(this.context.logger(), "resource_pack.missing_font_texture");
        textureToEquipments.report(this.context.logger(), "resource_pack.missing_equipment_texture");
        oggToSoundEvents.report(this.context.logger(), "resource_pack.missing_sound");

        return ModelReferenceCollector.combine(modelReferences);
    }

    record ModelReferences(Multimap<Key, Key> items, Multimap<Key, String> blocks) {
    }
}
