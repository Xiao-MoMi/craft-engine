package net.momirealms.craftengine.core.pack.obfuscation;

import net.momirealms.craftengine.core.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public class ResourceKey {
    private final String namespace;
    private final String path;
    private final ResourceType resourceType;
    private boolean pngHasMcmeta;
    private boolean notAtlases;

    public ResourceKey(String namespace, String path, ResourceType resourceType, boolean pngHasMcmeta, boolean notAtlases) {
        this.namespace = namespace;
        this.path = path;
        this.resourceType = resourceType;
        this.pngHasMcmeta = pngHasMcmeta;
        this.notAtlases = notAtlases;
    }

    public ResourceKey(String namespace, String path, ResourceType resourceType) {
        this(namespace, path, resourceType, false, false);
    }

    public static ResourceKey of(String namespace, String path, ResourceType resourceType, boolean pngHasMcmeta, boolean notAtlases) {
        return new ResourceKey(namespace, path, resourceType, pngHasMcmeta, notAtlases);
    }

    public static ResourceKey of(String namespace, String path, ResourceType resourceType, boolean pngHasMcmeta) {
        return ResourceKey.of(namespace, path, resourceType, pngHasMcmeta, false);
    }

    public static ResourceKey of(String namespace, String path, ResourceType resourceType) {
        return ResourceKey.of(namespace, path, resourceType, false);
    }

    public static ResourceKey of(String path, ResourceType resourceType, boolean pngHasMcmeta, boolean notAtlases) {
        if (path.contains(":")) {
            String[] parts = path.split(":");
            return ResourceKey.of(parts[0], parts[1], resourceType, pngHasMcmeta, notAtlases);
        }
        return ResourceKey.of("minecraft", path, resourceType, pngHasMcmeta, notAtlases);
    }

    public static ResourceKey of(String path, ResourceType resourceType, boolean pngHasMcmeta) {
        return ResourceKey.of(path, resourceType, pngHasMcmeta, false);
    }

    public static ResourceKey of(String path, ResourceType resourceType) {
        return ResourceKey.of(path, resourceType, false);
    }

    @Nullable
    public static ResourceKey from(Path path, Path rootPath) {
        if (!FileUtils.inNamespaceFolder(path, rootPath)) return null;
        Path relativePath = rootPath.relativize(path).normalize();
        if (!Files.isRegularFile(path)) return null;
        if (relativePath.getFileName().toString().endsWith(".mcmeta")) return null;
        if (relativePath.getName(0).toString().equals("assets") &&
                Arrays.asList("sounds.json", "gpu_warnlist.json", "regional_compliancies.json")
                        .contains(relativePath.getName(2).toString())
        ) return null;
        relativePath = relativePath.subpath(1, relativePath.getNameCount());
        String namespace = relativePath.getName(0).toString();
        relativePath = relativePath.subpath(1, relativePath.getNameCount());
        ResourceType resourceType;
        try {
            resourceType = ResourceType.of(relativePath.getName(0).toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
        relativePath = relativePath.subpath(1, relativePath.getNameCount());
        String value = relativePath.toString().replace(relativePath.getFileSystem().getSeparator(), "/");
        boolean hasMcmeta = false;
        if (relativePath.getFileName().toString().endsWith(".png")) {
            hasMcmeta = Files.exists(path.resolveSibling(relativePath.getFileName() + ".mcmeta"));
        }
        return ResourceKey.of(namespace, value, resourceType, hasMcmeta);
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    public ResourceType resourceType() {
        return resourceType;
    }

    public boolean pngHasMcmeta() {
        return pngHasMcmeta;
    }

    public boolean notAtlases() {
        return notAtlases;
    }

    public void setPngHasMcmeta(boolean pngHasMcmeta) {
        this.pngHasMcmeta = pngHasMcmeta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceKey that = (ResourceKey) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(path, that.path) &&
                Objects.equals(resourceType, that.resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                namespace,
                path,
                resourceType
        );
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    public Path toPath(Path rootPath) {
        return rootPath
                .resolve("assets")
                .resolve(namespace)
                .resolve(resourceType.typeName())
                .resolve(path + resourceType.suffix());
    }

    public boolean hasFile(Path rootPath) {
        return Files.exists(toPath(rootPath));
    }
}
