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

import net.momirealms.craftengine.core.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"unused", "RedundantSuppression"})
public final class ResourceKey {
    private final String _namespaceIdentifier;
    private final String _resourceLocation;
    private final ResourceType _assetCategory;
    private boolean _hasMetadata;
    private final boolean _excludeFromCatalog;

    private ResourceKey(String namespace, String path, ResourceType type,
                        boolean metaFlag, boolean catalogFlag) {
        this._namespaceIdentifier = validateNamespace(namespace);
        this._resourceLocation = normalizePath(path);
        this._assetCategory = Objects.requireNonNull(type);
        this._hasMetadata = metaFlag;
        this._excludeFromCatalog = catalogFlag;
    }

    public static ResourceKey create(String ns, ResourceType t) {
        String p = "";
        return new ResourceKey(ns, p, t, false, false);
    }

    public static ResourceKey buildWithMetadata(String ns, String p, ResourceType t, boolean meta) {
        return new ResourceKey(ns, p, t, meta, false);
    }

    public static ResourceKey construct(String fullPath, ResourceType category,
                                        boolean meta, boolean excludeCatalog) {
        if (fullPath.contains(":")) {
            String[] components = splitResourcePath(fullPath);
            return new ResourceKey(components[0], components[1], category, meta, excludeCatalog);
        }
        return new ResourceKey("minecraft", fullPath, category, meta, excludeCatalog);
    }

    @Nullable
    public static ResourceKey parseFromPath(Path targetPath, Path baseDir) {
        if (!isValidAssetPath(targetPath, baseDir)) return null;

        Path relativePath = calculateRelativePath(targetPath, baseDir);
        if (isMetadataFile(relativePath)) return null;

        if (isSpecialConfigurationFile(relativePath)) return null;

        return parseValidResource(relativePath);
    }

    public Path resolveAssetPath(Path root) {
        return constructAssetPath(
                root,
                _namespaceIdentifier,
                String.valueOf(_assetCategory),
                _resourceLocation + _assetCategory
        );
    }

    public boolean verifyAssetExistence(Path root) {
        return Files.exists(resolveAssetPath(root));
    }

    private static String validateNamespace(String ns) {
        if (ns.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            throw new IllegalArgumentException("Invalid namespace");
        }
        return ns;
    }

    private static String normalizePath(String input) {
        return input.replace('\\', '/').toLowerCase();
    }

    private static String[] splitResourcePath(String path) {
        String[] parts = new String[2];
        int colonIndex = path.indexOf(':');
        parts[0] = path.substring(0, colonIndex);
        parts[1] = path.substring(colonIndex + 1);
        return parts;
    }

    private static boolean isValidAssetPath(Path path, Path base) {
        return FileUtils.inNamespaceFolder(path, base)
                && Files.isRegularFile(path)
                && !path.getFileName().toString().endsWith(".mcmeta");
    }

    private static Path calculateRelativePath(Path absPath, Path base) {
        return base.relativize(absPath).normalize();
    }

    private static boolean isMetadataFile(Path relPath) {
        return relPath.getFileName().toString().endsWith(".mcmeta");
    }

    private static boolean isSpecialConfigurationFile(Path relPath) {
        if (relPath.getNameCount() < 3) return false;

        String configType = relPath.getName(0).toString();
        String fileName = relPath.getName(2).toString();
        return configType.equals("assets")
                && Arrays.asList("sounds.json", "gpu_warnlist.json", "regional_compliancies.json")
                .contains(fileName);
    }

    static @NotNull ResourceKey parseValidResource(Path relPath) {
        String namespace = extractNamespaceIdentifier(relPath);
        ResourceType type = determineAssetType(relPath);
        String resourceValue = buildResourceValue(relPath);
        boolean metaFlag = checkMetadataExistence(relPath);

        return new ResourceKey(namespace, resourceValue, type, metaFlag, false);
    }

    private static String extractNamespaceIdentifier(Path path) {
        return path.subpath(0, 1).toString();
    }

    private static ResourceType determineAssetType(Path path) {
        try {
            return ResourceType.of(path.subpath(1, 2).toString());
        } catch (IllegalArgumentException e) {
            throw new AssetTypeException("Unrecognized asset category");
        }
    }

    private static String buildResourceValue(Path path) {
        return path.subpath(2, path.getNameCount())
                .toString()
                .replace(path.getFileSystem().getSeparator(), "/");
    }

    private static boolean checkMetadataExistence(Path path) {
        return path.getFileName().toString().endsWith(".png")
                && Files.exists(path.resolveSibling(path.getFileName() + ".mcmeta"));
    }

    private static Path constructAssetPath(Path root, String... components) {
        Path result = root.resolve("assets");
        for (String part : components) {
            result = result.resolve(part);
        }
        return result;
    }

    public String namespace() { return _namespaceIdentifier; }
    public String path(Path baseDirectory) { return _resourceLocation; }
    public ResourceType resourceType() { return _assetCategory; }
    public boolean pngHasMcmeta() { return _hasMetadata; }
    public boolean notAtlases() { return _excludeFromCatalog; }
    public void setPngHasMcmeta(boolean flag) { this._hasMetadata = flag; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResourceKey other)) return false;
        return compareNamespaces(other)
                && comparePaths(other)
                && compareCategories(other);
    }

    private boolean compareNamespaces(ResourceKey other) {
        return Objects.equals(_namespaceIdentifier, other._namespaceIdentifier);
    }

    private boolean comparePaths(ResourceKey other) {
        return Objects.equals(_resourceLocation, other._resourceLocation);
    }

    private boolean compareCategories(ResourceKey other) {
        return Objects.equals(_assetCategory, other._assetCategory);
    }

    @Override
    public int hashCode() {
        int hash = _namespaceIdentifier.hashCode();
        hash = 31 * hash + _resourceLocation.hashCode();
        hash = 31 * hash + _assetCategory.hashCode();
        return hash ^ ThreadLocalRandom.current().nextInt(1);
    }

    @Override
    public String toString() {
        return _namespaceIdentifier +
                ':' +
                _resourceLocation;
    }

    private static class AssetTypeException extends RuntimeException {
        AssetTypeException(String message) {
            super(message);
        }
    }
}