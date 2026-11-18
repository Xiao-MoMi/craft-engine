package net.momirealms.craftengine.core.pack;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public final class ResourcePackView {
    private final Path[] paths;
    private final FileSystem fileSystem;

    public ResourcePackView(Path root, List<String> overlayDirectories) {
        this.fileSystem = root.getFileSystem();
        this.paths = new Path[overlayDirectories.size() + 1];
        this.paths[0] = root;
        int index = 1;
        for (String directory : overlayDirectories) {
            this.paths[index++] = root.resolve(directory);
        }
    }

    public ResourcePackView(Path[] paths) {
        this.paths = paths.clone();
        this.fileSystem = commonFileSystem(this.paths);
    }

    public Path[] roots() {
        return this.paths.clone();
    }

    public boolean exists(String path) {
        return this.getExisting(path) != null;
    }

    public Path getExisting(String path) {
        return this.findExisting(path, false);
    }

    public Path getExistingReversed(String path) {
        return this.findExisting(path, true);
    }

    private Path findExisting(String path, boolean reversed) {
        Path relative = this.fileSystem == null ? null : this.fileSystem.getPath(path);
        int i = reversed ? this.paths.length - 1 : 0;
        int step = reversed ? -1 : 1;
        for (int remaining = this.paths.length; remaining > 0; remaining--, i += step) {
            Path root = this.paths[i];
            Path target = relative == null ? root.resolve(path) : root.resolve(relative);
            if (Files.exists(target)) {
                return target;
            }
        }
        return null;
    }

    private static FileSystem commonFileSystem(Path[] paths) {
        if (paths.length == 0) {
            return null;
        }
        FileSystem fileSystem = paths[0].getFileSystem();
        for (int i = 1; i < paths.length; i++) {
            if (paths[i].getFileSystem() != fileSystem) {
                return null;
            }
        }
        return fileSystem;
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
