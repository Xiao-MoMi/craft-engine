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
    private final Set<String> hasNamespace = new HashSet<>();
    private final Set<String> hasPath = new HashSet<>();
    private static final Random random = new Random();
    private static final Gson gson = new Gson();
    private static final List<String> happyString = Arrays.asList("con", "prn", "aux", "nul");

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

    private String getRandomPath(int length, int obfuscateLevel) {
        StringBuilder path = new StringBuilder(atlasesEntry + "/");
        boolean isAddHappyString = false;
        length -= path.length();
        while (length >= 3) {
            if (obfuscateLevel == 3 && !isAddHappyString) {
                int remainingLoops = ((length - 6) / 2) + 1;
                if (random.nextInt(remainingLoops) == 0) {
                    String addHappyString = happyString.get(random.nextInt(happyString.size())) + "/";
                    path.append(addHappyString);
                    length -= addHappyString.length();
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
        return path.toString();
    }

    public ResourceKey getRandomResourceKey(int length, ResourceType resourceType,
                                            boolean pngHasMcmeta, int obfuscateLevel) {
        String namespace = getRandomNamespace();
        if (pngHasMcmeta) length -= 7;
        length -= namespace.length() + 1;
        length -= resourceType.typeName().length() + 1;
        length -= resourceType.suffix().length();
        String path = getRandomPath(length, obfuscateLevel);
        return ResourceKey.of(namespace, path, resourceType, pngHasMcmeta);
    }

    public void writeAtlasesJson(Path rootPath) {
        Path path = rootPath.resolve("assets/minecraft/atlases/blocks.json");
        Dictionary<String, List<Dictionary<String, String>>> atlasesJson = new Hashtable<>();
        Dictionary<String, String> source = new Hashtable<>() {{
            put("type", "directory");
            put("source", atlasesEntry);
            put("prefix", atlasesEntry + "/");
        }};
        atlasesJson.put("sources", List.of(source));
        if (!Files.exists(path)) {
            try (JsonWriter writer = new JsonWriter(new FileWriter(path.toFile()))) {
                gson.toJson(atlasesJson, atlasesJson.getClass(), writer);
            } catch (IOException e) {
                e.printStackTrace();
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
                } catch (IOException e1) {
                    e.printStackTrace();
                }
            }
        }
    }
}
