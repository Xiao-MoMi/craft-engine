package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class BedrockResourceSharing {
    private BedrockResourceSharing() {
    }

    public static void share(Path root) throws IOException {
        share(root, root.resolve(RegistrationPlanKind.BLOCK.file()), null);
    }

    static void share(Path root, Path blockPlan, @Nullable Map<Path, JsonObject> modelDocuments) throws IOException {
        // 展示方块可能在后续场景中补充姿态，必须等所有资源定稿后再共享；缓存只存活于本次生成。
        Set<String> animatedTiles = new HashSet<>();
        List<References> references = readReferences(root, blockPlan, animatedTiles);
        Set<String> textures = new HashSet<>();
        Set<String> geometries = new HashSet<>();
        for (References reference : references) {
            reference.textures().forEach(value -> collectStrings(value.value(), textures));
            reference.geometries().forEach(value -> collectStrings(value.value(), geometries));
        }
        List<Path> redundantFiles = new ArrayList<>();
        Map<String, String> textureAliases = shareTextures(root, textures, animatedTiles, redundantFiles);
        Map<String, String> geometryAliases = shareGeometries(root, geometries, redundantFiles, modelDocuments);
        for (References reference : references) {
            boolean changed = false;
            for (Reference value : reference.textures()) {
                changed |= value.replace(textureAliases);
            }
            for (Reference value : reference.geometries()) {
                changed |= value.replace(geometryAliases);
            }
            if (changed) {
                GsonHelper.writeJsonFile(reference.document(), reference.path());
            }
        }
        for (Path file : redundantFiles) {
            Files.delete(file);
        }
    }

    private static Map<String, String> shareTextures(Path root, Set<String> references, Set<String> animatedTiles, List<Path> redundantFiles) throws IOException {
        Map<String, String> pathsByHash = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();
        Path nativeTextures = root.resolve(BedrockPackLayout.NATIVE_MODEL_TEXTURES);
        byte[] buffer = new byte[16 * 1024];
        for (Path file : files(root.resolve(BedrockPackLayout.CRAFTENGINE_TEXTURES), ".png")) {
            if (file.startsWith(nativeTextures)) {
                continue;
            }
            String path = root.relativize(file).toString().replace('\\', '/');
            String texture = path.substring(0, path.length() - 4);
            if (!references.contains(texture) && !references.contains(path)) {
                continue;
            }
            // flipbook 会更新地形图集槽位；首帧相同不代表整段动画相同，保留这些槽位的独立路径。
            if (animatedTiles.contains(texture) || animatedTiles.contains(path)) {
                continue;
            }
            String shared = pathsByHash.putIfAbsent(HashUtils.sha1(file, buffer), path);
            // 哈希只用于索引；逐字节确认相同，保留 PNG 元数据及透明像素的原始语义。
            if (shared != null && FileUtils.contentEquals(root.resolve(shared), file, buffer)) {
                aliases.put(path, shared);
                aliases.put(texture, shared.substring(0, shared.length() - 4));
                redundantFiles.add(file);
            }
        }
        return aliases;
    }

    private static Map<String, String> shareGeometries(Path root, Set<String> references, List<Path> redundantFiles,
                                                       @Nullable Map<Path, JsonObject> modelDocuments) throws IOException {
        Map<GeometryKey, String> geometries = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();
        Path blockModels = root.resolve(BedrockDefinitionBundler.BLOCK_MODELS);
        Path entityModels = root.resolve(BedrockDefinitionBundler.ENTITY_MODELS);
        for (Path file : files(root.resolve("models"), ".geo.json")) {
            JsonObject document = GsonHelper.readJsonFromFile(file).getAsJsonObject();
            JsonArray definitions = document.getAsJsonArray("minecraft:geometry");
            if (definitions == null) {
                continue;
            }
            JsonObject header = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : document.entrySet()) {
                if (!entry.getKey().equals("minecraft:geometry")) {
                    header.add(entry.getKey(), entry.getValue());
                }
            }
            boolean changed = false;
            for (Iterator<JsonElement> iterator = definitions.iterator(); iterator.hasNext();) {
                JsonObject geometry = iterator.next().getAsJsonObject();
                JsonObject description = geometry.getAsJsonObject("description");
                String id = description.get("identifier").getAsString();
                if (!references.contains(id) || !(id.startsWith(BedrockPackLayout.GEOMETRY_PREFIX) || id.startsWith("geometry.item_display."))) {
                    continue;
                }
                // 仅忽略标识符；格式版本、骨骼名、UV、材质槽和所有显示变换都参与比较。
                // 其余子节点已定稿，可共享只读引用，无需深拷贝整个网格。
                JsonObject shape = new JsonObject();
                geometry.entrySet().forEach(entry -> shape.add(entry.getKey(), entry.getValue()));
                JsonObject sharedDescription = description.deepCopy();
                sharedDescription.remove("identifier");
                shape.add("description", sharedDescription);
                String previous = geometries.putIfAbsent(new GeometryKey(header, shape), id);
                if (previous != null) {
                    aliases.put(id, previous);
                    iterator.remove();
                    changed = true;
                }
            }
            if (changed && definitions.isEmpty()) {
                redundantFiles.add(file);
            } else if (modelDocuments != null && (file.startsWith(blockModels) || file.startsWith(entityModels))) {
                modelDocuments.put(file, document);
            } else if (changed) {
                GsonHelper.writeJsonFile(document, file);
            }
        }
        return aliases;
    }

    private static List<References> readReferences(Path root, Path blockPlan, Set<String> animatedTiles) throws IOException {
        List<References> result = new ArrayList<>();
        for (String directory : List.of("attachables", "entity")) {
            String type = directory.equals("attachables") ? "minecraft:attachable" : "minecraft:client_entity";
            for (Path file : files(root.resolve(directory), ".json")) {
                JsonObject document = GsonHelper.readJsonFromFile(file).getAsJsonObject();
                JsonObject description = document.getAsJsonObject(type).getAsJsonObject("description");
                References reference = new References(file, document, new ArrayList<>(), new ArrayList<>());
                if (description.has("textures")) {
                    reference.textures().add(new Reference(description, "textures"));
                }
                if (description.has("geometry")) {
                    reference.geometries().add(new Reference(description, "geometry"));
                }
                result.add(reference);
            }
        }
        JsonObject terrain = null;
        for (String atlas : List.of(BedrockPackLayout.ITEM_TEXTURE_ATLAS, BedrockPackLayout.TERRAIN_TEXTURE_ATLAS)) {
            Path file = root.resolve(atlas);
            if (Files.exists(file)) {
                JsonObject document = GsonHelper.readJsonFromFile(file).getAsJsonObject();
                if (atlas.equals(BedrockPackLayout.TERRAIN_TEXTURE_ATLAS)) {
                    terrain = document.getAsJsonObject("texture_data");
                }
                List<Reference> textures = new ArrayList<>();
                document.getAsJsonObject("texture_data").entrySet().forEach(entry -> textures.add(new Reference(entry.getValue().getAsJsonObject(), "textures")));
                result.add(new References(file, document, textures, List.of()));
            }
        }
        Path flipbooks = root.resolve(BedrockPackLayout.FLIPBOOK_TEXTURES);
        if (Files.exists(flipbooks)) {
            JsonArray document = GsonHelper.readJsonFromFile(flipbooks).getAsJsonArray();
            List<Reference> textures = new ArrayList<>(document.size());
            for (JsonElement element : document) {
                // 只替换贴图路径，atlas_tile、帧顺序、时长和插值开关均保持原值。
                JsonObject flipbook = element.getAsJsonObject();
                textures.add(new Reference(flipbook, "flipbook_texture"));
                String tile = flipbook.get("atlas_tile").getAsString();
                if (terrain != null && terrain.has(tile)) {
                    collectStrings(terrain.getAsJsonObject(tile).get("textures"), animatedTiles);
                }
            }
            result.add(new References(flipbooks, document, textures, List.of()));
        }
        if (Files.exists(blockPlan)) {
            JsonObject document = GsonHelper.readJsonFromFile(blockPlan).getAsJsonObject();
            List<Reference> geometries = new ArrayList<>();
            document.getAsJsonArray(BedrockPackLayout.ENTRIES).forEach(entry -> geometries.add(new Reference(entry.getAsJsonObject(), BedrockPackLayout.GEOMETRY)));
            result.add(new References(blockPlan, document, List.of(), geometries));
        }
        return result;
    }

    private static List<Path> files(Path directory, String suffix) throws IOException {
        if (!Files.exists(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(file -> Files.isRegularFile(file) && file.toString().endsWith(suffix)).sorted().toList();
        }
    }

    private static void collectStrings(JsonElement value, Set<String> result) {
        if (value instanceof JsonObject object) {
            object.entrySet().forEach(entry -> collectStrings(entry.getValue(), result));
        } else if (value instanceof JsonArray array) {
            array.forEach(element -> collectStrings(element, result));
        } else if (value instanceof JsonPrimitive primitive && primitive.isString()) {
            result.add(primitive.getAsString());
        }
    }

    private record References(Path path, JsonElement document, List<Reference> textures, List<Reference> geometries) {
    }

    private record Reference(JsonObject object, String key) {
        private JsonElement value() {
            return this.object.get(this.key);
        }

        private boolean replace(Map<String, String> aliases) {
            return GsonHelper.replaceStringValues(this.object, this.key, aliases);
        }
    }

    private record GeometryKey(JsonObject header, JsonObject definition) {
    }
}
