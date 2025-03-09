package net.momirealms.craftengine.core.pack.obfuscation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RandomResourceKey {
    private String atlasesEntry;
    private String randomName;
    private final List<String> cacheNamespace = new ArrayList<>();
    private final Set<String> hasNamespace = new HashSet<>();
    private final Set<String> hasPath = new HashSet<>();
    private static final Random random = new Random();
    private static final Gson gson = new Gson();

    public RandomResourceKey(String atlasesEntry, String randomName) {
        if (randomName == null || !randomName.matches("^[a-z0-9_.-]+$")) {
            throw new IllegalArgumentException(randomName);
        }
        this.atlasesEntry = atlasesEntry;
        this.randomName = randomName.toLowerCase(Locale.ROOT);
    }

    public RandomResourceKey() {
        this("ce", "abcdefghijklmnopqrstuvwxyz");
    }

    public String atlasesEntry() {
        return atlasesEntry;
    }

    public String randomNameList() {
        return randomName;
    }

    public void setAtlasesEntry(String atlasesEntry) {
        this.atlasesEntry = atlasesEntry;
    }

    public void setRandomName(String randomName) {
        this.randomName = randomName;
    }

    public String getRandomNamespace(int obfuscateLevel, int namespaceAmount) {
        if (obfuscateLevel == 3) {
            return getRandomNamespace();
        } else {
            if (cacheNamespace.size() < namespaceAmount) {
                for (int i = 0; i < namespaceAmount; i++) {
                    cacheNamespace.add(getRandomNamespace());
                }
            }
            return cacheNamespace.get(random.nextInt(cacheNamespace.size()));
        }
    }

    private String getRandomNamespace() {
        String namespace = "";
        while (true) {
            namespace += randomName.charAt(random.nextInt(randomName.length()));
            if (!hasNamespace.contains(namespace)) {
                hasNamespace.add(namespace);
                return namespace;
            }
        }
    }

    private String getRandomPath(int length, boolean antiUnzip) {
        StringBuilder path = new StringBuilder(atlasesEntry + "/");
        boolean isAddHappyString = false;
        length -= path.length();
        while (length >= 3) {
            if (antiUnzip && !isAddHappyString) {
                int remainingLoops = ((length - 6) / 2) + 1;
                if (random.nextInt(remainingLoops) == 0) {
                    path.append(".../");
                    length -= 4;
                    isAddHappyString = true;
                    continue;
                }
            }
            path.append(randomName.charAt(random.nextInt(randomName.length()))).append("/");
            length -= 2;
        }
        switch (length) {
            case 1:
                path.append(randomName.charAt(random.nextInt(randomName.length())));
                break;
            case 2:
                path.append(randomName.charAt(random.nextInt(randomName.length())))
                        .append(randomName.charAt(random.nextInt(randomName.length())));
                break;
            default:
                break;
        }
        if (hasPath.contains(path.toString())) {
            return getRandomPath(length, antiUnzip);
        }
        hasPath.add(path.toString());
        return path.toString();
    }

    public ResourceKey getRandomResourceKey(int length, ResourceType resourceType,
                                            boolean pngHasMcmeta, int obfuscateLevel,
                                            int namespaceAmount, boolean antiUnzip) {
        length -= 7;
        if (pngHasMcmeta) length -= 7;
        String namespace = getRandomNamespace(obfuscateLevel, namespaceAmount);
        length -= namespace.length() + 1;
        length -= resourceType.typeName().length() + 1;
        length -= resourceType.suffix().length();
        try {
            String path = getRandomPath(length, antiUnzip);
            return ResourceKey.of(namespace, path, resourceType, pngHasMcmeta);
        } catch (StackOverflowError | OutOfMemoryError e) {
            throw new RuntimeException("Please increase the value of resource-pack.obfuscation.path-length in config.yml" + e);
        }
    }

    public void writeAtlasesJson(Path rootPath) throws IOException {
        Path path = rootPath.resolve("assets/minecraft/atlases/blocks.json");
        Dictionary<String, List<Dictionary<String, String>>> atlasesJson = new Hashtable<>();
        Dictionary<String, String> source = new Hashtable<>() {{
            put("type", "directory");
            put("source", atlasesEntry);
            put("prefix", atlasesEntry + "/");
        }};
        atlasesJson.put("sources", List.of(source));
        if (!Files.exists(path)) {
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            try (JsonWriter writer = new JsonWriter(new FileWriter(path.toFile()))) {
                gson.toJson(atlasesJson, atlasesJson.getClass(), writer);
            }
        }
        else {
            try (JsonReader reader = new JsonReader(new FileReader(path.toFile()))) {
                JsonObject existing = gson.fromJson(reader, JsonObject.class);
                JsonArray sources = existing.getAsJsonArray("sources");
                if (!sources.contains(gson.toJsonTree(source))) {
                    sources.add(gson.toJsonTree(source));
                }
                try (JsonWriter writer = new JsonWriter(new FileWriter(path.toFile()))) {
                    gson.toJson(existing, writer);
                }
            } catch (Exception e) {
                try (JsonWriter writer = new JsonWriter(new FileWriter(path.toFile()))) {
                    gson.toJson(atlasesJson, atlasesJson.getClass(), writer);
                }
            }
        }
    }
}
