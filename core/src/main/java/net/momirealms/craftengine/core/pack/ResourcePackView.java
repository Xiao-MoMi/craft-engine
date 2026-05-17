package net.momirealms.craftengine.core.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public final class ResourcePackView {
    private final Path[] paths;
    private final int pathCount;

    public ResourcePackView(Path[] paths) {
        this.paths = paths;
        this.pathCount = paths.length;
    }

    public boolean exists(String path) {
        for (int i = 0; i < this.pathCount; i++) {
            if (Files.exists(paths[i].resolve(path))) {
                return true;
            }
        }
        return false;
    }

    public Path getExisting(String path) {
        for (int i = 0; i < this.pathCount; i++) {
            Path target = this.paths[i].resolve(path);
            if (Files.exists(target)) {
                return target;
            }
        }
        return null;
    }

    public Path getExistingReversed(String path) {
        for (int i = this.pathCount - 1; i >= 0; i--) {
            Path target = this.paths[i].resolve(path);
            if (Files.exists(target)) {
                return target;
            }
        }
        return null;
    }

    public <T> T getExisting(String path, Function<Path, T> function) {
        Path filePath = getExisting(path);
        if (filePath == null) {
            return null;
        }
        return function.apply(filePath);
    }

    public <T> T getExistingReversed(String path, Function<Path, T> function) {
        Path filePath = getExistingReversed(path);
        if (filePath == null) {
            return null;
        }
        return function.apply(filePath);
    }
}
