package net.momirealms.craftengine.core.pack.obfuscation;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ZipUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProtectHelper {
    private static final Gson gson = new Gson();

    private static Set<ResourceKey> getAllResourceKeys(Path rootPath) throws IOException {
        Set<ResourceKey> allResourceKeys = new HashSet<>();
        for (Path path : FileUtils.getAllFiles(rootPath)) {
            if (JsonUtils.isJsonFile(path, false)) {
                try {
                    JsonObject jsonData = gson.fromJson(Files.readString(path), JsonObject.class);
                    Set<ResourceKey> resourceKeys = JsonUtils.getAllResourceKeys(jsonData);
                    allResourceKeys.addAll(resourceKeys);
                } catch (JsonSyntaxException ignored) {}
            }
        }
        for (ResourceKey resourceKey : allResourceKeys) {
            if (resourceKey.resourceType() == ResourceType.TEXTURE) {
                Path path = resourceKey.toPath(rootPath);
                if (path.toString().endsWith(".png")) {
                    resourceKey.setPngHasMcmeta(Files.exists(path.resolveSibling(path.getFileName() + ".mcmeta")));
                }
            }
        }
        return allResourceKeys;
    }

    private static Map<ResourceKey, ResourceKey> buildReplaceMap(
            Path rootPath, Set<ResourceKey> allResourceKeys,
            RandomResourceKey randomResourceKey, int length,
            int obfuscateLevel
    ) throws IOException {
        Map<ResourceKey, ResourceKey> allReplaceMap = new HashMap<>();
        for (ResourceKey resourceKey : allResourceKeys) {
            if (!resourceKey.hasFile(rootPath)) continue;
            ResourceKey newResourceKey = randomResourceKey.getRandomResourceKey(
                    length,
                    resourceKey.resourceType(),
                    resourceKey.pngHasMcmeta(),
                    obfuscateLevel
            );
            allReplaceMap.put(resourceKey, newResourceKey);
        }
        return allReplaceMap;
    }

    public static void protect(Path rootPath, Path zipPath) throws IOException {
        Section protectionSettings = CraftEngine.instance().configManager().settings()
                .getSection("resource-pack")
                .getSection("protection");
        int protectZipLevel = protectionSettings.getInt("protection");
        int obfuscateLevel = protectionSettings.getInt("obfuscation");
        int length = protectionSettings.getInt("obfuscation-path-length");
        Map<ResourceKey, ResourceKey> allReplaceMap = null;
        if (obfuscateLevel > 0 && protectZipLevel > 0) {
            if (obfuscateLevel == 2) length = 65528;
            RandomResourceKey randomResourceKey = new RandomResourceKey();
            randomResourceKey.writeAtlasesJson(rootPath);
            Set<ResourceKey> allResourceKeys = getAllResourceKeys(rootPath);
            allReplaceMap = buildReplaceMap(
                    rootPath, allResourceKeys, randomResourceKey,
                    length, obfuscateLevel
            );
        }
        ZipUtils.zipDirectory(rootPath, zipPath, protectZipLevel, allReplaceMap);
    }
}

