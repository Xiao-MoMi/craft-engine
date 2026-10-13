package net.momirealms.craftengine.core.pack.model.bbmodel;

import com.google.gson.*;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.pack.Identifier;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class BBModelConverter {
    private static final String BASE64_PNG_PREFIX = "data:image/png;base64,";
    private static final String EXTENSION = ".bbmodel";
    private static final int[] NEW_ROTATION_SINCE = new int[]{1, 21, 11};

    private BBModelConverter() {
    }

    public record ResolvedBlueprint(Path file, String relativePath) {}

    public static ResolvedBlueprint resolveBlueprint(Pack pack, Path configFile, String blueprint) {
        boolean hasExtension = blueprint.endsWith(EXTENSION);
        Path file = blueprintFolderFor(pack, configFile).resolve(hasExtension ? blueprint : blueprint + EXTENSION);
        String relativePath = CharacterUtils.replaceBackslashWithSlash(hasExtension ? blueprint.substring(0, blueprint.length() - EXTENSION.length()) : blueprint);
        return new ResolvedBlueprint(file, relativePath);
    }

    public static Path blueprintFolderFor(Pack pack, Path configFile) {
        Path[] configDirs = pack.configurationFolders();
        Path[] blueprintDirs = pack.blueprintFolders();
        if (blueprintDirs.length == 1) {
            return blueprintDirs[0];
        }
        for (int i = 0; i < configDirs.length; i++) {
            if (configFile.startsWith(configDirs[i])) {
                return blueprintDirs[i];
            }
        }
        return blueprintDirs[0];
    }

    public static Converted convert(ResolvedBlueprint blueprint, String namespace, String folder, String node) {
        String modelPath = folder + "/" + blueprint.relativePath();
        if (!Identifier.isValidPath(modelPath)) {
            throw new KnownResourceException("resource.model.bbmodel.invalid_name", node, blueprint.relativePath());
        }
        return convert(blueprint.file(), Key.of(namespace, modelPath), node);
    }

    public static Converted convert(Pack pack, Path configFile, String folder, @Nullable ConfigValue pathValue, ConfigValue blueprintValue) {
        ResolvedBlueprint blueprint = resolveBlueprint(pack, configFile, blueprintValue.getAsString());
        if (pathValue != null) {
            return convert(blueprint.file(), pathValue.getAsAssetPath(), blueprintValue.path());
        }
        return convert(blueprint, pack.namespace(), folder, blueprintValue.path());
    }

    public static Converted convert(Path bbmodelFile, Key modelKey, String node) {
        JsonObject root;
        try {
            root = GsonHelper.readJsonObjectFromFile(bbmodelFile);
            if (root == null) {
                throw new KnownResourceException("resource.model.bbmodel.invalid_file", node, bbmodelFile.toAbsolutePath().toString());
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new KnownResourceException("resource.model.bbmodel.invalid_file", node, bbmodelFile.toAbsolutePath().toString());
        }

        // Project resolution，uv 换算和 texture_size 均基于它（而非贴图像素尺寸）
        JsonObject resolution = root.getAsJsonObject("resolution");
        float resolutionWidth = resolution != null ? resolution.get("width").getAsFloat() : 16f;
        float resolutionHeight = resolution != null ? resolution.get("height").getAsFloat() : 16f;

        // 贴图
        List<JsonObject> textures = new ArrayList<>();
        JsonArray texturesArray = root.getAsJsonArray(ModelJsonKeys.TEXTURES);
        if (texturesArray != null) {
            for (JsonElement element : texturesArray) {
                textures.add(element.getAsJsonObject());
            }
        }

        // 元素。Blockbench 会丢弃没有任何已贴图面的元素
        boolean rotationLimit = compareVersion(getString(root, "java_block_version"), NEW_ROTATION_SINCE) < 0;
        // 1.9.0 的 rotation_snap：受限格式下角度对齐到 22.5 的倍数
        boolean rotationSnap = "1.9.0".equals(getString(root, "java_block_version"));
        JsonArray elementsOut = new JsonArray();
        Set<Integer> usedTextures = new HashSet<>();
        JsonArray elements = root.getAsJsonArray(ModelJsonKeys.ELEMENTS);
        if (elements != null) {
            for (JsonElement element : elements) {
                JsonObject cube = element.getAsJsonObject();
                if (cube.has("type") && !"cube".equals(cube.get("type").getAsString())) continue;
                if (cube.has("export") && !cube.get("export").getAsBoolean()) continue;
                JsonObject converted = convertElement(cube, textures, usedTextures, resolutionWidth, resolutionHeight, rotationLimit, rotationSnap);
                if (converted != null) {
                    elementsOut.add(converted);
                }
            }
        }

        // 贴图引用。Blockbench 只导出被面引用的贴图（particle 除外）；
        // 无元素但有 parent 的纯贴图模型导出全部贴图
        boolean texturesOnlyModel = elementsOut.isEmpty() && !getString(root, ModelJsonKeys.PARENT).isEmpty();
        int particleIndex = -1;
        for (int i = 0; i < textures.size(); i++) {
            if (textures.get(i).has("particle") && textures.get(i).get("particle").getAsBoolean()) {
                particleIndex = i;
                break;
            }
        }
        Set<Integer> exportedTextures = new HashSet<>(usedTextures);
        if (particleIndex != -1) exportedTextures.add(particleIndex);
        int embeddedTotal = 0;
        for (int i = 0; i < textures.size(); i++) {
            if (isEmbedded(textures.get(i)) && (exportedTextures.contains(i) || texturesOnlyModel)) embeddedTotal++;
        }
        List<String> textureLinks = new ArrayList<>(textures.size());
        Map<Key, byte[]> embeddedTextures = new LinkedHashMap<>();
        Map<Key, JsonObject> textureMetadata = new LinkedHashMap<>();
        int embeddedIndex = 0;
        for (int i = 0; i < textures.size(); i++) {
            JsonObject texture = textures.get(i);
            if (isEmbedded(texture) && (exportedTextures.contains(i) || texturesOnlyModel)) {
                embeddedIndex++;
                byte[] png;
                JsonObject metadata;
                try {
                    png = Base64.getDecoder().decode(texture.get("source").getAsString().substring(BASE64_PNG_PREFIX.length()));
                    metadata = TextureAnimationMetadata.create(texture, png, resolutionWidth, resolutionHeight);
                } catch (IOException | IllegalArgumentException e) {
                    throw new KnownResourceException("resource.model.bbmodel.invalid_file", node, bbmodelFile.toAbsolutePath().toString());
                }
                Key textureKey = Key.of(modelKey.namespace(), embeddedTotal == 1 ? modelKey.value() : modelKey.value() + "_" + embeddedIndex);
                embeddedTextures.put(textureKey, png);
                if (metadata != null) textureMetadata.put(textureKey, metadata);
                textureLinks.add(textureKey.asMinimalString());
            } else {
                textureLinks.add(javaTextureLink(texture));
            }
        }

        JsonObject modelJson = new JsonObject();
        String version = getString(root, "java_block_version");
        if (!version.isEmpty()) {
            modelJson.addProperty("format_version", version);
        }
        String credit = getString(root, "credit");
        if (!credit.isEmpty()) {
            modelJson.addProperty("credit", credit);
        }
        String parent = getString(root, ModelJsonKeys.PARENT);
        if (!parent.isEmpty()) {
            modelJson.addProperty(ModelJsonKeys.PARENT, parent);
        }
        if (root.has(ModelJsonKeys.AMBIENT_OCCLUSION) && !root.get(ModelJsonKeys.AMBIENT_OCCLUSION).getAsBoolean()) {
            modelJson.addProperty(ModelJsonKeys.AMBIENT_OCCLUSION, false);
        }
        if (resolution != null && (resolutionWidth != 16f || resolutionHeight != 16f)) {
            JsonArray textureSize = new JsonArray();
            textureSize.add(resolution.get("width").getAsInt());
            textureSize.add(resolution.get("height").getAsInt());
            modelJson.add("texture_size", textureSize);
        }
        if (!textures.isEmpty()) {
            JsonObject texturesOut = new JsonObject();
            for (int i = 0; i < textures.size(); i++) {
                String link = textureLinks.get(i);
                // particle 无论是否被引用都导出
                JsonObject texture = textures.get(i);
                if (texture.has("particle") && texture.get("particle").getAsBoolean()) {
                    texturesOut.addProperty(ModelJsonKeys.PARTICLE, link);
                }
                if (!usedTextures.contains(i) && !texturesOnlyModel) continue;
                // Blockbench 在贴图 id 与 link 相同时不写出该条目
                String id = textureId(texture, i);
                if (!id.equals(stripLeadingHash(link))) {
                    texturesOut.addProperty(id, link);
                }
            }
            if (!texturesOut.isEmpty()) {
                modelJson.add(ModelJsonKeys.TEXTURES, texturesOut);
            }
        }
        if (!elementsOut.isEmpty()) {
            modelJson.add(ModelJsonKeys.ELEMENTS, elementsOut);
        }
        if (root.has("front_gui_light") && root.get("front_gui_light").getAsBoolean()) {
            modelJson.addProperty(ModelJsonKeys.GUI_LIGHT, "front");
        }
        if (root.has("overrides") && root.get("overrides").isJsonArray()) {
            modelJson.add("overrides", root.getAsJsonArray("overrides"));
        }
        if (root.has(ModelJsonKeys.DISPLAY)) {
            modelJson.add(ModelJsonKeys.DISPLAY, root.getAsJsonObject(ModelJsonKeys.DISPLAY));
        }
        JsonObject unhandled = root.getAsJsonObject("unhandled_root_fields");
        if (unhandled != null) {
            for (Map.Entry<String, JsonElement> entry : unhandled.entrySet()) {
                if (!modelJson.has(entry.getKey())) {
                    modelJson.add(entry.getKey(), entry.getValue());
                }
            }
        }
        return new Converted(modelKey, modelJson, embeddedTextures, textureMetadata);
    }

    @Nullable
    private static JsonObject convertElement(JsonObject cube, List<JsonObject> textures, Set<Integer> usedTextures, float resolutionWidth, float resolutionHeight, boolean rotationLimit, boolean rotationSnap) {
        JsonObject out = new JsonObject();
        String name = getString(cube, "name");
        if (!name.isEmpty() && !"cube".equals(name)) {
            out.addProperty("name", name);
        }
        JsonArray from = cube.getAsJsonArray(ModelJsonKeys.FROM).deepCopy();
        JsonArray to = cube.getAsJsonArray(ModelJsonKeys.TO).deepCopy();
        // Blockbench 将 inflate 烘焙进 from/to，原版元素没有 inflate 字段
        if (cube.has("inflate")) {
            float inflate = cube.get("inflate").getAsFloat();
            for (int i = 0; i < 3; i++) {
                from.set(i, new JsonPrimitive(from.get(i).getAsFloat() - inflate));
                to.set(i, new JsonPrimitive(to.get(i).getAsFloat() + inflate));
            }
        }
        out.add(ModelJsonKeys.FROM, from);
        out.add(ModelJsonKeys.TO, to);
        if (cube.has(ModelJsonKeys.SHADE) && !cube.get(ModelJsonKeys.SHADE).getAsBoolean()) {
            out.addProperty(ModelJsonKeys.SHADE, false);
        }
        if (cube.has(ModelJsonKeys.LIGHT_EMISSION)) {
            int lightEmission = cube.get(ModelJsonKeys.LIGHT_EMISSION).getAsInt();
            if (lightEmission != 0) {
                out.addProperty(ModelJsonKeys.LIGHT_EMISSION, lightEmission);
            }
        }
        JsonArray rotation = cube.getAsJsonArray(ModelJsonKeys.ROTATION);
        float rx = rotation != null ? rotation.get(0).getAsFloat() : 0f;
        float ry = rotation != null ? rotation.get(1).getAsFloat() : 0f;
        float rz = rotation != null ? rotation.get(2).getAsFloat() : 0f;
        int nonZeroAxes = (rx != 0 ? 1 : 0) + (ry != 0 ? 1 : 0) + (rz != 0 ? 1 : 0);
        JsonArray origin = cube.getAsJsonArray(ModelJsonKeys.ORIGIN);
        boolean originNonZero = origin != null && (origin.get(0).getAsFloat() != 0 || origin.get(1).getAsFloat() != 0 || origin.get(2).getAsFloat() != 0);
        boolean rescale = cube.has(ModelJsonKeys.RESCALE) && cube.get(ModelJsonKeys.RESCALE).getAsBoolean();
        JsonObject rotationOut = null;
        // Blockbench 默认 java_export_pivots=true：旋转非零，或 origin 非零时都会写出 rotation
        if (nonZeroAxes > 0 || originNonZero) {
            rotationOut = new JsonObject();
            // 1.21.11+ 允许多轴或任意角度，使用 {x,y,z} 新格式；旧版本仅单轴且 [-45,45]
            if (!rotationLimit && (nonZeroAxes > 1 || Math.abs(rx) > 45 || Math.abs(ry) > 45 || Math.abs(rz) > 45)) {
                rotationOut.addProperty(ModelJsonKeys.X, rx);
                rotationOut.addProperty(ModelJsonKeys.Y, ry);
                rotationOut.addProperty(ModelJsonKeys.Z, rz);
            } else {
                // 受限格式取第一个非零轴；全零时用 cube 记忆的 rotation_axis（默认 y）
                String axis;
                float angle;
                if (rx != 0) {
                    axis = "x";
                    angle = rx;
                } else if (ry != 0) {
                    axis = "y";
                    angle = ry;
                } else if (rz != 0) {
                    axis = "z";
                    angle = rz;
                } else {
                    axis = rotationAxis(cube);
                    angle = 0;
                }
                if (rotationSnap) {
                    angle = Math.round(angle / 22.5f) * 22.5f;
                }
                rotationOut.addProperty(ModelJsonKeys.ANGLE, angle);
                rotationOut.addProperty(ModelJsonKeys.AXIS, axis);
            }
            rotationOut.add(ModelJsonKeys.ORIGIN, origin != null ? origin.deepCopy() : zeroVector());
        }
        if (rescale) {
            if (rotationOut != null) {
                rotationOut.addProperty(ModelJsonKeys.RESCALE, true);
            } else {
                rotationOut = new JsonObject();
                rotationOut.addProperty(ModelJsonKeys.ANGLE, 0);
                rotationOut.addProperty(ModelJsonKeys.AXIS, rotationAxis(cube));
                rotationOut.add(ModelJsonKeys.ORIGIN, origin != null ? origin.deepCopy() : zeroVector());
                rotationOut.addProperty(ModelJsonKeys.RESCALE, true);
            }
        }
        if (rotationOut != null) {
            out.add(ModelJsonKeys.ROTATION, rotationOut);
        }
        // 旧版本遇上多轴旋转时，Blockbench 不报错，而是把完整三轴存进非标准 rotated 字段以便重新导入
        if (rotationLimit && nonZeroAxes >= 2) {
            out.add("rotated", rotation.deepCopy());
        }
        JsonObject facesOut = new JsonObject();
        JsonObject faces = cube.getAsJsonObject(ModelJsonKeys.FACES);
        if (faces != null) {
            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                // Blockbench 跳过未分配贴图的面
                if (!face.has(ModelJsonKeys.TEXTURE) || face.get(ModelJsonKeys.TEXTURE).isJsonNull()) continue;
                int textureIndex = face.get(ModelJsonKeys.TEXTURE).getAsInt();
                JsonObject faceOut = new JsonObject();
                if (!face.has("enabled") || face.get("enabled").getAsBoolean()) {
                    JsonArray uv = face.getAsJsonArray(ModelJsonKeys.UV);
                    if (uv != null) {
                        JsonArray uvOut = new JsonArray();
                        uvOut.add(uv.get(0).getAsFloat() * 16f / resolutionWidth);
                        uvOut.add(uv.get(1).getAsFloat() * 16f / resolutionHeight);
                        uvOut.add(uv.get(2).getAsFloat() * 16f / resolutionWidth);
                        uvOut.add(uv.get(3).getAsFloat() * 16f / resolutionHeight);
                        faceOut.add(ModelJsonKeys.UV, uvOut);
                    }
                }
                if (face.has(ModelJsonKeys.ROTATION) && face.get(ModelJsonKeys.ROTATION).getAsInt() != 0) {
                    faceOut.add(ModelJsonKeys.ROTATION, face.get(ModelJsonKeys.ROTATION));
                }
                // 贴图引用解析不到时 Blockbench 保留该面并写 #missing
                if (textureIndex >= 0 && textureIndex < textures.size()) {
                    usedTextures.add(textureIndex);
                    faceOut.addProperty(ModelJsonKeys.TEXTURE, "#" + textureId(textures.get(textureIndex), textureIndex));
                } else {
                    faceOut.addProperty(ModelJsonKeys.TEXTURE, "#missing");
                }
                if (face.has(ModelJsonKeys.CULL_FACE) && !face.get(ModelJsonKeys.CULL_FACE).getAsString().isEmpty()) {
                    faceOut.addProperty(ModelJsonKeys.CULL_FACE, face.get(ModelJsonKeys.CULL_FACE).getAsString());
                }
                if (face.has("tint") && face.get("tint").getAsInt() >= 0) {
                    faceOut.addProperty(ModelJsonKeys.TINT_INDEX, face.get("tint").getAsInt());
                }
                facesOut.add(entry.getKey(), faceOut);
            }
        }
        if (facesOut.isEmpty()) {
            return null;
        }
        out.add(ModelJsonKeys.FACES, facesOut);
        return out;
    }

    /**
     * 与 Blockbench Texture#javaTextureLink 一致：name 去扩展名 + folder 前缀，
     * 仅当 namespace 非空且非 minecraft 时加命名空间前缀
     */
    private static String javaTextureLink(JsonObject texture) {
        String name = getString(texture, "name").replaceAll("\\.\\w{2,8}$", "");
        String folder = getString(texture, "folder");
        String namespace = getString(texture, "namespace");
        String link = folder.isEmpty() ? name : folder + "/" + name;
        if (!namespace.isEmpty() && !"minecraft".equals(namespace)) {
            link = namespace + ":" + link;
        }
        return link;
    }

    /**
     * 贴图在 JSON textures 中的键。Blockbench 使用贴图的 id 字段，面通过 '#id' 引用
     */
    private static String textureId(JsonObject texture, int index) {
        String id = getString(texture, "id");
        return id.isEmpty() ? String.valueOf(index) : id;
    }

    private static boolean isEmbedded(JsonObject texture) {
        return texture.has("source") && texture.get("source").getAsString().startsWith(BASE64_PNG_PREFIX);
    }

    private static String getString(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : "";
    }

    /**
     * 比较版本号与给定版本，返回负数表示 version 更低。缺失或非法时视为最新（返回正数）
     */
    private static int compareVersion(String version, int[] target) {
        if (version.isEmpty()) return 1;
        String[] parts = version.split("\\.");
        for (int i = 0; i < target.length; i++) {
            int segment;
            try {
                segment = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            } catch (NumberFormatException e) {
                return 1;
            }
            if (segment != target[i]) return segment - target[i];
        }
        return 0;
    }

    private static String rotationAxis(JsonObject cube) {
        String axis = getString(cube, "rotation_axis");
        return switch (axis) {
            case "x", "y", "z" -> axis;
            default -> "y";
        };
    }

    private static JsonArray zeroVector() {
        JsonArray array = new JsonArray();
        array.add(0);
        array.add(0);
        array.add(0);
        return array;
    }

    private static String stripLeadingHash(String link) {
        return link.startsWith("#") ? link.substring(1) : link;
    }

    public record Converted(Key model, JsonObject json, Map<Key, byte[]> textures, Map<Key, JsonObject> textureMetadata) {
        public Converted(Key model, JsonObject json, Map<Key, byte[]> textures) {
            this(model, json, textures, Map.of());
        }
    }
}
