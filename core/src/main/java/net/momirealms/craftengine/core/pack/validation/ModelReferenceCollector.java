package net.momirealms.craftengine.core.pack.validation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.validation.ResourceValidation.ModelReferences;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.craftengine.core.util.ResourcePackJsonUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class ModelReferenceCollector {
    private static final int EXPECTED_REFERENCES_PER_MODEL = 2;
    private static final ModelReferences EMPTY = new ModelReferences(ImmutableSetMultimap.of(), ImmutableSetMultimap.of());
    private final ValidationContext context;
    private final Set<Path> reusableRoots;
    private final Map<Path, ModelReferences> cached = new HashMap<>();

    ModelReferenceCollector(ValidationContext context, Set<Path> reusableRoots) {
        this.context = context;
        this.reusableRoots = reusableRoots;
    }

    static ModelReferences combine(List<ModelReferences> roots) {
        if (roots.isEmpty()) {
            return EMPTY;
        }
        if (roots.size() == 1) {
            return roots.getFirst();
        }
        Multimap<Key, Key> items = HashMultimap.create(128, EXPECTED_REFERENCES_PER_MODEL);
        Multimap<Key, String> blocks = HashMultimap.create(128, EXPECTED_REFERENCES_PER_MODEL);
        for (ModelReferences references : roots) {
            items.putAll(references.items());
            blocks.putAll(references.blocks());
        }
        return new ModelReferences(items, blocks);
    }

    private static void collectMultipart(JsonArray jsonArray, Consumer<Key> callback) {
        for (JsonElement element : jsonArray) {
            if (element instanceof JsonObject jo) {
                JsonElement applyJE = jo.get("apply");
                if (applyJE instanceof JsonObject applyJO) {
                    String modelPath = applyJO.get("model").getAsString();
                    Key location = Key.from(modelPath);
                    callback.accept(location);
                } else if (applyJE instanceof JsonArray applyJA) {
                    for (JsonElement applyInnerJE : applyJA) {
                        if (applyInnerJE instanceof JsonObject applyInnerJO) {
                            String modelPath = applyInnerJO.get("model").getAsString();
                            Key location = Key.from(modelPath);
                            callback.accept(location);
                        }
                    }
                }
            }
        }
    }

    private static void collectVariants(String block, JsonObject jsonObject, BiConsumer<Key, String> callback) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getValue() instanceof JsonObject entryJO) {
                String modelPath = entryJO.get("model").getAsString();
                Key location = Key.from(modelPath);
                callback.accept(location, block + "[" + entry.getKey() + "]");
            } else if (entry.getValue() instanceof JsonArray entryJA) {
                for (JsonElement entryInnerJE : entryJA) {
                    if (entryInnerJE instanceof JsonObject entryJO) {
                        String modelPath = entryJO.get("model").getAsString();
                        Key location = Key.from(modelPath);
                        callback.accept(location, block + "[" + entry.getKey() + "]");
                    }
                }
            }
        }
    }

    private static void collectItemModelsDeeply(JsonObject jo, Consumer<Key> callback) {
        JsonElement modelJE = jo.get("model");
        if (modelJE instanceof JsonPrimitive jsonPrimitive) {
            Key location = Key.from(jsonPrimitive.getAsString());
            callback.accept(location);
            return;
        }
        if (jo.has("type") && jo.has("base")) {
            if (jo.get("type") instanceof JsonPrimitive jp1 && jo.get("base") instanceof JsonPrimitive jp2) {
                String type = jp1.getAsString();
                if (type.equals("minecraft:special") || type.equals("special")) {
                    Key location = Key.from(jp2.getAsString());
                    callback.accept(location);
                }
            }
        }
        for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
            if (entry.getValue() instanceof JsonObject innerJO) {
                collectItemModelsDeeply(innerJO, callback);
            } else if (entry.getValue() instanceof JsonArray innerJA) {
                for (JsonElement innerElement : innerJA) {
                    if (innerElement instanceof JsonObject innerJO) {
                        collectItemModelsDeeply(innerJO, callback);
                    }
                }
            }
        }
    }

    ModelReferences collect(Path root, List<Path> namespaces) {
        ModelReferences cached = this.cached.get(root);
        if (cached != null) {
            return cached;
        }
        Multimap<Key, Key> modelToItemDefinitions = HashMultimap.create(128, EXPECTED_REFERENCES_PER_MODEL);
        Multimap<Key, String> modelToBlockStates = HashMultimap.create(128, EXPECTED_REFERENCES_PER_MODEL);
        MutableBoolean complete = new MutableBoolean(true);
        for (Path namespacePath : namespaces) {
            String namespace = namespacePath.getFileName().toString();
            // 1.21.4+的物品模型
            Path itemsPath = namespacePath.resolve("items");
            if (Files.isDirectory(itemsPath)) {
                try {
                    Files.walkFileTree(itemsPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                            if (!FileUtils.isJsonFile(file)) {
                                return FileVisitResult.CONTINUE;
                            }
                            Key item = Key.of(namespace, FileUtils.pathWithoutExtension(file.getFileName().toString()));
                            if (!ModelReferenceCollector.this.collectItem(file, (identifier) -> modelToItemDefinitions.put(identifier, item))) {
                                complete.set(false);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    complete.set(false);
                    this.context.logger().warn("Failed to walk through items", e);
                }
            }

            // 方块状态json
            Path blockStatesPath = namespacePath.resolve("blockstates");
            if (Files.isDirectory(blockStatesPath)) {
                try {
                    Files.walkFileTree(blockStatesPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                            if (!FileUtils.isJsonFile(file)) {
                                return FileVisitResult.CONTINUE;
                            }
                            String blockId = FileUtils.pathWithoutExtension(file.getFileName().toString());
                            if (!ModelReferenceCollector.this.collectBlockState(file, blockId, modelToBlockStates::put)) {
                                complete.set(false);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    complete.set(false);
                    this.context.logger().warn("Failed to walk through blockstates", e);
                }
            }
        }
        ModelReferences references = new ModelReferences(Multimaps.unmodifiableMultimap(modelToItemDefinitions), Multimaps.unmodifiableMultimap(modelToBlockStates));
        // Failed reads are retried so later segments retain their diagnostics.
        // Only input references are shared; repairable model JSON is never retained here.
        if (complete.booleanValue() && this.reusableRoots.contains(root)) {
            this.cached.put(root, references);
        }
        return references;
    }

    void release(Path root) {
        this.cached.remove(root);
    }

    private boolean collectItem(Path file, Consumer<Key> callback) {
        List<String> models;
        try {
            models = ResourcePackJsonUtils.readJsonObjectOrWarn(this.context.logger(), file, ModelReferenceReader::readItem);
        } catch (ModelReferenceReader.TreeReadingRequired e) {
            JsonObject json = this.context.readJson(file);
            if (json == null) {
                return false;
            }
            collectItemModelsDeeply(json, callback);
            return true;
        }
        if (models == null) {
            return false;
        }
        for (String model : models) {
            callback.accept(Key.from(model));
        }
        return true;
    }

    private boolean collectBlockState(Path file, String block, BiConsumer<Key, String> callback) {
        List<ModelReferenceReader.BlockReference> models;
        try {
            models = ResourcePackJsonUtils.readJsonObjectOrWarn(this.context.logger(), file, ModelReferenceReader::readBlockState);
        } catch (ModelReferenceReader.TreeReadingRequired e) {
            JsonObject json = this.context.readJson(file);
            if (json == null) {
                return false;
            }
            if (json.has("multipart")) {
                collectMultipart(json.getAsJsonArray("multipart"), (location) -> callback.accept(location, block));
            } else if (json.has("variants")) {
                collectVariants(block, json.getAsJsonObject("variants"), callback);
            }
            return true;
        }
        if (models == null) {
            return false;
        }
        for (ModelReferenceReader.BlockReference model : models) {
            String referrer = model.variant() == null ? block : block + "[" + model.variant() + "]";
            callback.accept(Key.from(model.model()), referrer);
        }
        return true;
    }

}
