/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.pack.obfuscation;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.JsonUtils;
import net.momirealms.craftengine.core.util.ZipUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public final class ProtectHelper {
    private static final Gson JSON_PARSER = new Gson();
    private static final int MAX_PATH_LENGTH = 0xFFFF;

    private static void validateConfiguration(int archiveProtectionLevel, int obfuscationLevel,
                                              int pathComplexity, int namespacePoolSize) {
        boolean validRange = pathComplexity > 0 && pathComplexity <= MAX_PATH_LENGTH
                && namespacePoolSize > 0;
        boolean validLevels = archiveProtectionLevel >= 0 && archiveProtectionLevel <= 3
                && obfuscationLevel >= 0 && obfuscationLevel <= 3;

        if (!validRange || !validLevels) {
            throw new SecurityConfigurationException("Invalid protection settings");
        }
    }

    public static void protect(Path sourceDir, Path outputArchive) throws IOException {
        Section protectionConfig = getSecuritySettings().getSection("protection");
        Section obfuscationConfig = getSecuritySettings().getSection("obfuscation");

        int archiveSecurityLevel = protectionConfig.getInt("level");
        int obfuscationComplexity = obfuscationConfig.getInt("level");
        boolean antiExtraction = obfuscationConfig.getBoolean("anti-unzip");
        int pathLengthSetting = obfuscationConfig.getInt("path-length");
        int namespacePool = obfuscationConfig.getInt("namespace-amount");

        validateConfiguration(archiveSecurityLevel, obfuscationComplexity,
                pathLengthSetting, namespacePool);

        Map<?, ?> resourceMapping = shouldObfuscate(obfuscationComplexity, archiveSecurityLevel)
                ? generateResourceMapping(sourceDir, pathLengthSetting, obfuscationComplexity, namespacePool, antiExtraction)
                : Collections.emptyMap();

        packageResources(sourceDir, outputArchive, archiveSecurityLevel, (Map<ResourceKey, ResourceKey>) resourceMapping);
    }

    private static @NotNull ConcurrentMap<Path, ResourceKey> generateResourceMapping(Path rootDir, int pathComplexity,
                                                                                     int securityLevel, int namespacePoolSize,
                                                                                     boolean antiExtractFlag) throws IOException {
        RandomResourceKey generator = new RandomResourceKey();
        generator.writeAtlasesJson(rootDir);

        List<Path> assetIdentifiers = scanResourceIdentifiers(rootDir);
        return createObfuscationMap(rootDir, assetIdentifiers, generator,
                pathComplexity, securityLevel, namespacePoolSize, antiExtractFlag);
    }

    private static List<Path> scanResourceIdentifiers(Path baseDir) throws IOException {
        return FileUtils.getAllFiles(baseDir);
    }

    private static Set<ResourceKey> parseJsonResources(Object jsonFile) {
        try {
            JsonObject data = JSON_PARSER.fromJson(Files.readString((Path) jsonFile), JsonObject.class);
            Set<ResourceKey> identifiers = JsonUtils.getAllResourceKeys(data);
            identifiers.forEach(key -> processTextureMetadata(key, (Path) jsonFile));
            return identifiers;
        } catch (JsonSyntaxException | IOException e) {
            return Collections.emptySet();
        }
    }

    private static void processTextureMetadata(ResourceKey identifier, Path contextPath) {
        if (identifier.resourceType() == ResourceType.TEXTURE) {
            Path texturePath = contextPath.resolveSibling(identifier.path(null));
            boolean hasMetadata = Files.exists(texturePath.resolveSibling(
                    texturePath.getFileName() + ".mcmeta"));
            identifier.setPngHasMcmeta(hasMetadata);
        }
    }

    private static @NotNull ConcurrentMap<Path, ResourceKey> createObfuscationMap(Path baseDir, List<Path> identifiers,
                                                                                  RandomResourceKey generator, int pathComplexity,
                                                                                  int securityLevel, int namespacePoolSize,
                                                                                  boolean antiExtractFlag) {
        return identifiers.parallelStream()
                .filter(key -> key.endsWith(baseDir))
                .collect(Collectors.toConcurrentMap(
                        key -> key,
                        key -> generator.getRandomResourceKey(
                                pathComplexity, ResourceKey.parseValidResource(key), securityLevel,
                                namespacePoolSize, antiExtractFlag
                        )
                ));
    }

    private static Section getSecuritySettings() {
        return CraftEngine.instance().configManager().settings()
                .getSection("resource-pack");
    }

    private static boolean shouldObfuscate(int obfuscationLevel, int protectionLevel) {
        return obfuscationLevel > 0 && protectionLevel > 0;
    }

    private static void packageResources(Path source, Path dest, int compressionLevel,
                                         Map<ResourceKey, ResourceKey> mapping) throws IOException {
        ZipUtils.zipDirectory(source, dest, compressionLevel, mapping);
    }

    private static class SecurityConfigurationException extends RuntimeException {
        SecurityConfigurationException(String message) {
            super(message + " in configuration");
        }
    }
}