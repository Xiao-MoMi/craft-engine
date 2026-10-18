package net.momirealms.craftengine.core.pack.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Atlas {
    private static final Sprite MISSING_SPRITE = new Sprite(false, null, null);
    private static final Sprite GENERATED_SPRITE = new Sprite(true, null, null);
    private final List<Source> sources = new ArrayList<>();
    private final Map<Key, SingleSprite> singleSprites = new HashMap<>();
    private final Map<Key, Sprite> sprites = new HashMap<>();

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
                        this.singleSprites.put(sprite == null ? resourceId : Key.of(sprite.getAsString()),
                                new SingleSprite(resourceId, this.sources.size()));
                    }
                }
                case "unstitch", "minecraft:unstitch" -> {
                    if (!sourceJson.has("resource")) {
                        continue;
                    }
                    UnstitchSource unstitch = UnstitchSource.fromJson(sourceJson);
                    Map<Key, TextureRegion> regions = new HashMap<>();
                    for (UnstitchSource.Region region : unstitch.regions()) {
                        regions.put(region.sprite(), new TextureRegion(unstitch.resource(), unstitch.divisorX(), unstitch.divisorY(), region));
                    }
                    this.sources.add(new RegionSource(regions));
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
        return resolveSprite(texture).defined;
    }

    public void addSingle(Key key) {
        this.addSingle(key, key);
    }

    public void addSingle(Key sprite, Key resource) {
        this.sprites.put(sprite, new Sprite(true, resource, null));
    }

    public void addDeleted(Key key) {
        this.sprites.put(key, MISSING_SPRITE);
    }

    @Nullable
    public Key getSourceTexturePath(Key texture) {
        return resolveSprite(texture).resource;
    }

    @Nullable
    public TextureRegion getTextureRegion(Key texture) {
        return resolveSprite(texture).region;
    }

    private Sprite resolveSprite(Key texture) {
        Sprite cached = this.sprites.get(texture);
        if (cached != null) {
            return cached;
        }
        Sprite sprite = this.findSprite(texture);
        this.sprites.put(texture, sprite);
        return sprite;
    }

    private Sprite findSprite(Key texture) {
        SingleSprite single = this.singleSprites.get(texture);
        // Only sources after the last matching single can override it.
        // The boundary counts non-single sources, so unrelated singles never need scanning.
        int firstSource = single == null ? 0 : single.sourceCount;
        for (int i = this.sources.size() - 1; i >= firstSource; i--) {
            Source source = this.sources.get(i);
            if (source.matches(texture)) {
                if (!source.providesSprite()) {
                    return MISSING_SPRITE;
                }
                Key resource = source.resource(texture);
                TextureRegion region = source.region(texture);
                return resource == null && region == null ? GENERATED_SPRITE : new Sprite(true, resource, region);
            }
        }
        return single == null ? MISSING_SPRITE : new Sprite(true, single.resource, null);
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

        @Nullable
        default TextureRegion region(Key texture) {
            return null;
        }
    }

    private record Sprite(boolean defined, @Nullable Key resource, @Nullable TextureRegion region) {
    }

    public record TextureRegion(Key resource, double divisorX, double divisorY, UnstitchSource.Region region) {
    }

    private record RegionSource(Map<Key, TextureRegion> regions) implements Source {

        @Override
        public boolean matches(Key texture) {
            return this.regions.containsKey(texture);
        }

        @Override
        public TextureRegion region(Key texture) {
            return this.regions.get(texture);
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

    private record SingleSprite(Key resource, int sourceCount) {
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
