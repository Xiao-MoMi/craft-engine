package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Atlas {
    private final Map<String, String> directory;
    private final String[] prefixes;
    // 已经被包含在图集内的贴图
    private final Set<Key> defined;
    // 单独添加的
    private final Set<Key> single;
    // 删除的
    private final Predicate<Key> filtered;
    // 所需准备的图集贴图
    private final Set<Key> unstitch;
    private final Set<Key> palettedPermutations;
    // 被CE移除的
    private final Set<Key> deleted;

    public Atlas(JsonObject atlasJson) {
        this.directory = new LinkedHashMap<>();
        this.single = new HashSet<>();
        this.defined = new HashSet<>();
        this.unstitch = new HashSet<>();
        this.deleted = new HashSet<>();
        this.palettedPermutations = new HashSet<>();
        List<Predicate<Key>> filtered = new ArrayList<>();
        JsonArray sources = atlasJson.getAsJsonArray("sources");
        if (sources != null) {
            for (JsonElement source : sources) {
                if (!(source instanceof JsonObject sourceJson)) continue;
                String type = Optional.ofNullable(sourceJson.get("type")).map(JsonElement::getAsString).orElse(null);
                if (type == null) continue;
                switch (type) {
                    case "directory", "minecraft:directory" -> {
                        JsonElement textureSource = sourceJson.get("source");
                        JsonElement texturePrefix = sourceJson.get("prefix");
                        if (texturePrefix == null || textureSource == null) continue;
                        this.directory.put(texturePrefix.getAsString(), textureSource.getAsString() + "/");
                    }
                    case "single", "minecraft:single" -> {
                        JsonElement resource = sourceJson.get("resource");
                        if (resource == null) continue;
                        Key key = Key.of(resource.getAsString());
                        this.defined.add(key);
                        this.single.add(key);
                    }
                    case "unstitch", "minecraft:unstitch" -> {
                        JsonElement resource = sourceJson.get("resource");
                        if (resource == null) continue;
                        this.unstitch.add(Key.of(resource.getAsString()));
                        JsonArray regions = sourceJson.getAsJsonArray("regions");
                        if (regions != null) {
                            for (JsonElement region : regions) {
                                if (!(region instanceof JsonObject regionJson)) continue;
                                JsonElement sprite = regionJson.get("sprite");
                                if (sprite == null) continue;
                                this.defined.add(Key.of(sprite.getAsString()));
                            }
                        }
                    }
                    case "paletted_permutations", "minecraft:paletted_permutations" -> {
                        JsonArray textures = sourceJson.getAsJsonArray("textures");
                        if (textures == null) continue;
                        JsonObject permutationsJson = sourceJson.getAsJsonObject("permutations");
                        if (permutationsJson == null) continue;
                        String separator = sourceJson.has("separator") ? sourceJson.get("separator").getAsString() : "_";
                        Map<String, String> permutations = permutationsJson.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsString()));
                        for (JsonElement texture : textures) {
                            if (!(texture instanceof JsonPrimitive texturePath)) continue;
                            String texturePathString = texturePath.getAsString();
                            this.palettedPermutations.add(Key.of(texturePathString));
                            for (String permutation : permutations.keySet()) {
                                this.defined.add(Key.of(texturePathString + separator + permutation));
                            }
                        }
                        for (String permutation : permutations.values()) {
                            this.palettedPermutations.add(Key.of(permutation));
                        }
                    }
                    case "filter", "minecraft:filter" -> {
                        JsonElement pattern = sourceJson.get("pattern");
                        if (!(pattern instanceof JsonObject patternJson)) continue;
                        Predicate<String> namespacePredicate;
                        Predicate<String> pathPredicate;
                        if (patternJson.has("namespace")) {
                            Pattern compiledPattern = Pattern.compile(patternJson.get("namespace").getAsString());
                            namespacePredicate = s -> compiledPattern.matcher(s).find();
                        } else {
                            namespacePredicate = s -> true;
                        }
                        if (patternJson.has("path")) {
                            Pattern compiledPattern = Pattern.compile(patternJson.get("path").getAsString());
                            pathPredicate = s -> compiledPattern.matcher(s).find();
                        } else {
                            pathPredicate = s -> true;
                        }
                        filtered.add(s -> namespacePredicate.test(s.namespace()) && pathPredicate.test(s.value()));
                    }
                }
            }
        }
        this.filtered = MiscUtils.anyOf(filtered);
        this.prefixes = this.directory.keySet().toArray(new String[0]);
    }

    public Atlas(List<JsonObject> atlasJsons) {
        this(mergeAtlas(atlasJsons));
    }

    public static JsonObject mergeAtlas(@NotNull List<JsonObject> atlasJsons) {
        if (atlasJsons.isEmpty()) return new JsonObject();
        if (atlasJsons.size() == 1) return atlasJsons.getFirst();
        JsonObject atlasJson = new JsonObject();
        JsonArray newSources = new JsonArray();
        atlasJson.add("sources", newSources);
        for (JsonObject other : atlasJsons) {
            if (other == null) continue;
            JsonArray sources = other.getAsJsonArray("sources");
            if (sources != null) {
                newSources.addAll(sources);
            }
        }
        return atlasJson;
    }

    public boolean isDefined(Key texture) {
        if (this.filtered.test(texture)) return false;
        if (this.defined.contains(texture)) return true;
        String path = texture.value();
        for (String prefix : this.prefixes) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    public void addSingle(Key key) {
        this.single.add(key);
        this.defined.add(key);
    }

    public void addDeleted(Key key) {
        this.deleted.add(key);
    }

    // 获取贴图源文件路径，有些类型可能查不到
    public Key getSourceTexturePath(Key texture) {
        // 被筛选掉了
        if (this.filtered.test(texture)) return null;
        // 被修复过
        if (this.deleted.contains(texture)) return null;
        // single直接包含
        if (this.single.contains(texture)) return texture;
        // 被unstitch或者调色盘定义
        if (this.defined.contains(texture)) return null;
        String path = texture.value();
        // 路径匹配
        for (Map.Entry<String, String> entry : this.directory.entrySet()) {
            String prefix = entry.getKey();
            if (path.startsWith(prefix)) {
                return Key.of(texture.namespace(), entry.getValue() + path.substring(prefix.length()));
            }
        }
        return null;
    }
}
