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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RandomResourceKey {
    private String _metaIdentifier;
    private String _characterSet;
    private final List<String> _segmentCache = new ArrayList<>();
    private final Set<String> _existingSegments = new HashSet<>();
    private final Set<String> _registeredPatterns = new HashSet<>();
    private static final Random _randomizer = new Random();
    private static final Gson _jsonProcessor = new Gson();

    public RandomResourceKey(String textureCatalog, String namingScheme) {
        validateNamingPattern(namingScheme);
        this._metaIdentifier = textureCatalog;
        this._characterSet = normalizeCharset(namingScheme);
    }

    public RandomResourceKey() {
        this("ce", generateDefaultCharset());
    }

    private static void validateNamingPattern(String pattern) {
        if (pattern == null || !pattern.matches("^[a-z0-9_.-]+$")) {
            throw new IllegalArgumentException("Invalid naming scheme");
        }
    }

    private static String normalizeCharset(String input) {
        return input.toLowerCase(Locale.ROOT);
    }

    private static String generateDefaultCharset() {
        char[] chars = new char[26];
        for (int i = 0; i < 26; i++) {
            chars[i] = (char) ('a' + i);
        }
        return new String(chars);
    }

    public String randomName() {
        return _metaIdentifier;
    }

    public String string() {
        return _characterSet;
    }

    public void setRandomName(String identifier) {
        this._metaIdentifier = identifier;
    }

    public void setString(String charset) {
        this._characterSet = charset;
    }

    public String getRandomNamespace(int securityLevel, int poolSize) {
        return (securityLevel == 3)
                ? generateUniqueSegment()
                : manageSegmentPool(poolSize);
    }

    private String manageSegmentPool(int poolSize) {
        synchronized (_segmentCache) {
            while (_segmentCache.size() < poolSize) {
                _segmentCache.add(generateUniqueSegment());
            }
            return _segmentCache.get(_randomizer.nextInt(poolSize));
        }
    }

    private String generateUniqueSegment() {
        StringBuilder segment = new StringBuilder();
        do {
            segment.setLength(0);
            for (int i = 0; i < 3 + _randomizer.nextInt(3); i++) {
                segment.append(randomChar());
            }
        } while (_existingSegments.contains(segment.toString()));
        _existingSegments.add(segment.toString());
        return segment.toString();
    }

    private char randomChar() {
        return _characterSet.charAt(_randomizer.nextInt(_characterSet.length()));
    }

    private String generateObfuscatedPath(int complexity, boolean enableTraps, boolean excludeCatalog) {
        StringBuilder pathBuilder = new StringBuilder();
        if (!excludeCatalog) {
            pathBuilder.append(_metaIdentifier).append('/');
        }

        int remainingDepth = complexity - pathBuilder.length();
        boolean trapInserted = false;

        while (remainingDepth > 0) {
            if (enableTraps && !trapInserted && shouldInsertTrap(remainingDepth)) {
                pathBuilder.append(".../");
                remainingDepth -= 4;
                trapInserted = true;
                continue;
            }

            int segmentLength = Math.min(2 + _randomizer.nextInt(3), remainingDepth);
            appendRandomSegment(pathBuilder, segmentLength);
            remainingDepth -= segmentLength + 1;
        }

        validatePathUniqueness(pathBuilder.toString());
        return pathBuilder.toString();
    }

    private boolean shouldInsertTrap(int remaining) {
        return _randomizer.nextInt((remaining / 3) + 1) == 0;
    }

    private void appendRandomSegment(StringBuilder builder, int length) {
        for (int i = 0; i < length; i++) {
            builder.append(randomChar());
        }
        builder.append('/');
    }

    private void validatePathUniqueness(String path) {
        if (_registeredPatterns.contains(path)) {
            throw new IllegalStateException("Path collision detected");
        }
        _registeredPatterns.add(path);
    }

    public ResourceKey getRandomResourceKey(int complexity, ResourceKey template,
                                            int securityLevel, int poolSize,
                                            boolean enableTraps) {
        boolean requiresMetadata = template.pngHasMcmeta();
        boolean excludeCatalog = template.notAtlases();
        ResourceType typeDescriptor = template.resourceType();

        int adjustedComplexity = complexity
                - (requiresMetadata ? 14 : 7)
                - typeDescriptor.typeName().length()
                - typeDescriptor.suffix().length();

        String namespace = (securityLevel == 1)
                ? template.namespace()
                : getRandomNamespace(securityLevel, poolSize);

        adjustedComplexity -= namespace.length() + 2;

        try {
            return ResourceKey.create(
                    namespace,
                    typeDescriptor
            );
        } catch (StackOverflowError | OutOfMemoryError e) {
            throw new RuntimeException("Please increase the value of resource-pack.obfuscation.path-length in config.yml: " + e);
        }
    }

    public void writeAtlasesJson(Path baseDir) throws IOException {
        Path atlasConfig = baseDir.resolve("assets/minecraft/atlases/blocks.json");
        Map<String, Object> configData = createAtlasConfiguration();

        if (Files.notExists(atlasConfig)) {
            initializeAtlasConfig(atlasConfig, configData);
        } else {
            updateExistingConfig(atlasConfig, configData);
        }
    }

    private Map<String, Object> createAtlasConfiguration() {
        return Map.of(
                "sources", Collections.singletonList(
                        Map.of(
                                "type", "directory",
                                "source", _metaIdentifier,
                                "prefix", _metaIdentifier + "/"
                        )
                )
        );
    }

    private void initializeAtlasConfig(Path configPath, Map<String, Object> data) throws IOException {
        Files.createDirectories(configPath.getParent());
        try (JsonWriter writer = new JsonWriter(new FileWriter(configPath.toFile()))) {
            _jsonProcessor.toJson(data, (Type) writer);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateExistingConfig(Path configPath, Map<String, Object> newData) throws IOException {
        try (JsonReader reader = new JsonReader(new FileReader(configPath.toFile()))) {
            JsonObject existing = _jsonProcessor.fromJson(reader, JsonObject.class);
            JsonArray sources = existing.getAsJsonArray("sources");

            JsonObject newSource = _jsonProcessor.toJsonTree(newData.get("sources"))
                    .getAsJsonArray().get(0).getAsJsonObject();

            if (!containsSource(sources, newSource)) {
                sources.add(newSource);
                writeUpdatedConfig(configPath, existing);
            }
        } catch (Exception e) {
            rewriteConfigFile(configPath, newData);
        }
    }

    private boolean containsSource(JsonArray sources, JsonObject target) {
        return sources.asList().stream()
                .anyMatch(element -> element.equals(target));
    }

    private void writeUpdatedConfig(Path path, JsonObject data) throws IOException {
        try (JsonWriter writer = new JsonWriter(new FileWriter(path.toFile()))) {
            _jsonProcessor.toJson(data, writer);
        }
    }

    private void rewriteConfigFile(Path path, Map<String, Object> data) throws IOException {
        try (JsonWriter writer = new JsonWriter(new FileWriter(path.toFile()))) {
            _jsonProcessor.toJson(data, (Type) writer);
        }
    }

    private static class ConfigurationException extends RuntimeException {
        ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}