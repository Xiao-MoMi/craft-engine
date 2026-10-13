package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Atlas {
    private final List<Source> sources = new ArrayList<>();

    public Atlas(JsonObject atlasJson) {
        JsonArray sources = atlasJson.getAsJsonArray("sources");
        if (sources == null) {
            return;
        }
        for (JsonElement source : sources) {
            if (!(source instanceof JsonObject sourceJson)) {
                continue;
            }
            JsonElement type = sourceJson.get("type");
            if (type == null) {
                continue;
            }
            switch (type.getAsString()) {
                case "directory", "minecraft:directory" -> {
                    JsonElement resource = sourceJson.get("source");
                    JsonElement prefix = sourceJson.get("prefix");
                    if (resource != null && prefix != null) {
                        this.sources.add(new DirectorySource(prefix.getAsString(), resource.getAsString() + "/"));
                    }
                }
                case "single", "minecraft:single" -> {
                    JsonElement resource = sourceJson.get("resource");
                    if (resource != null) {
                        Key resourceId = Key.of(resource.getAsString());
                        JsonElement sprite = sourceJson.get("sprite");
                        this.addSingle(sprite == null ? resourceId : Key.of(sprite.getAsString()), resourceId);
                    }
                }
                case "unstitch", "minecraft:unstitch" -> {
                    if (!sourceJson.has("resource")) {
                        continue;
                    }
                    Set<Key> sprites = new HashSet<>();
                    JsonArray regions = sourceJson.getAsJsonArray("regions");
                    if (regions != null) {
                        for (JsonElement region : regions) {
                            if (region instanceof JsonObject regionJson && regionJson.has("sprite")) {
                                sprites.add(Key.of(regionJson.get("sprite").getAsString()));
                            }
                        }
                    }
                    this.sources.add(new GeneratedSource(sprites));
                }
                case "paletted_permutations", "minecraft:paletted_permutations" -> {
                    JsonArray textures = sourceJson.getAsJsonArray("textures");
                    JsonObject permutations = sourceJson.getAsJsonObject("permutations");
                    if (textures == null || permutations == null) {
                        continue;
                    }
                    String separator = sourceJson.has("separator") ? sourceJson.get("separator").getAsString() : "_";
                    Set<Key> sprites = new HashSet<>();
                    for (JsonElement texture : textures) {
                        if (texture instanceof JsonPrimitive texturePath) {
                            String path = texturePath.getAsString();
                            for (String permutation : permutations.keySet()) {
                                sprites.add(Key.of(path + separator + permutation));
                            }
                        }
                    }
                    this.sources.add(new GeneratedSource(sprites));
                }
                case "filter", "minecraft:filter" -> {
                    if (sourceJson.get("pattern") instanceof JsonObject pattern) {
                        Predicate<String> namespace = pattern.has("namespace") ? Pattern.compile(pattern.get("namespace").getAsString()).asPredicate() : value -> true;
                        Predicate<String> path = pattern.has("path") ? Pattern.compile(pattern.get("path").getAsString()).asPredicate() : value -> true;
                        this.sources.add(new FilterSource(key -> namespace.test(key.namespace()) && path.test(key.value())));
                    }
                }
            }
        }
    }

    public Atlas(List<JsonObject> atlasJsons) {
        this(mergeAtlas(atlasJsons));
    }

    public static JsonObject mergeAtlas(@NotNull List<JsonObject> atlasJsons) {
        if (atlasJsons.isEmpty()) {
            return new JsonObject();
        }
        if (atlasJsons.size() == 1) {
            return atlasJsons.getFirst();
        }
        JsonObject atlasJson = new JsonObject();
        JsonArray newSources = new JsonArray();
        atlasJson.add("sources", newSources);
        for (JsonObject other : atlasJsons) {
            if (other == null) {
                continue;
            }
            JsonArray sources = other.getAsJsonArray("sources");
            if (sources != null) {
                newSources.addAll(sources);
            }
        }
        return atlasJson;
    }

    public boolean isDefined(Key texture) {
        Source source = this.findSource(texture);
        return source != null && source.providesSprite();
    }

    public void addSingle(Key key) {
        this.addSingle(key, key);
    }

    public void addSingle(Key sprite, Key resource) {
        this.sources.add(new SingleSource(sprite, resource));
    }

    public void addDeleted(Key key) {
        this.sources.add(new FilterSource(key::equals));
    }

    @Nullable
    public Key getSourceTexturePath(Key texture) {
        Source source = this.findSource(texture);
        return source == null ? null : source.resource(texture);
    }

    @Nullable
    private Source findSource(Key texture) {
        // The last matching source determines whether a sprite was added, replaced, or removed.
        for (int i = this.sources.size() - 1; i >= 0; i--) {
            Source source = this.sources.get(i);
            if (source.matches(texture)) {
                return source;
            }
        }
        return null;
    }

    private interface Source {

        boolean matches(Key texture);

        default boolean providesSprite() {
            return true;
        }

        @Nullable
        default Key resource(Key texture) {
            return null;
        }
    }

    private record DirectorySource(String prefix, String directory) implements Source {

        @Override
        public boolean matches(Key texture) {
            return texture.value().startsWith(this.prefix);
        }

        @Override
        public Key resource(Key texture) {
            return Key.of(texture.namespace(), this.directory + texture.value().substring(this.prefix.length()));
        }
    }

    private record SingleSource(Key sprite, Key path) implements Source {

        @Override
        public boolean matches(Key texture) {
            return this.sprite.equals(texture);
        }

        @Override
        public Key resource(Key texture) {
            return this.path;
        }
    }

    private record GeneratedSource(Set<Key> sprites) implements Source {

        @Override
        public boolean matches(Key texture) {
            return this.sprites.contains(texture);
        }
    }

    private record FilterSource(Predicate<Key> predicate) implements Source {

        @Override
        public boolean providesSprite() {
            return false;
        }

        @Override
        public boolean matches(Key texture) {
            return this.predicate.test(texture);
        }
    }
}
