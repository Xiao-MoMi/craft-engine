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
            RandomResourceKey randomResourceKey, int pathLength,
            int obfuscateLevel, int namespaceAmount, boolean antiUnzip
    ) {
        Map<ResourceKey, ResourceKey> allReplaceMap = new HashMap<>();
        for (ResourceKey resourceKey : allResourceKeys) {
            if (!resourceKey.hasFile(rootPath)) continue;
            ResourceKey newResourceKey = randomResourceKey.getRandomResourceKey(
                    pathLength,
                    resourceKey,
                    obfuscateLevel,
                    namespaceAmount,
                    antiUnzip
            );
            allReplaceMap.put(resourceKey, newResourceKey);
        }
        return allReplaceMap;
    }

    private static void checkConfig(int protectZipLevel, int obfuscateLevel, int pathLength, int namespaceAmount) {
        if (pathLength > 0 && pathLength <= 65535 && namespaceAmount > 0
                && protectZipLevel >= 0 && protectZipLevel <= 3
                && obfuscateLevel >= 0 && obfuscateLevel <= 3
        ) return;
        throw new IllegalArgumentException("Please check whether the resource-pack.protection or resource-pack.obfuscation configurations in config.yml are valid");
    }

    public static void protect(Path rootPath, Path zipPath) throws IOException {
        Section protectionSettings = CraftEngine.instance().configManager().settings()
                .getSection("resource-pack")
                .getSection("protection");
        Section obfuscationSettings = CraftEngine.instance().configManager().settings()
                .getSection("resource-pack")
                .getSection("obfuscation");
        int protectZipLevel = protectionSettings.getInt("level");
        int obfuscateLevel = obfuscationSettings.getInt("level");
        boolean antiUnzip = obfuscationSettings.getBoolean("anti-unzip");
        int pathLength = obfuscationSettings.getInt("path-length");
        int namespaceAmount = obfuscationSettings.getInt("namespace-amount");
        checkConfig(protectZipLevel, obfuscateLevel, pathLength, namespaceAmount);
        Map<ResourceKey, ResourceKey> allReplaceMap = null;
        if (obfuscateLevel > 0 && protectZipLevel > 0) {
            RandomResourceKey randomResourceKey = new RandomResourceKey();
            randomResourceKey.writeAtlasesJson(rootPath);
            Set<ResourceKey> allResourceKeys = getAllResourceKeys(rootPath);
            allReplaceMap = buildReplaceMap(
                    rootPath, allResourceKeys, randomResourceKey,
                    pathLength, obfuscateLevel, namespaceAmount,
                    antiUnzip
            );
        }
        ZipUtils.zipDirectory(rootPath, zipPath, protectZipLevel, allReplaceMap);
    }
}

