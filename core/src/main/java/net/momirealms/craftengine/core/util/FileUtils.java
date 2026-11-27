package net.momirealms.craftengine.core.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class FileUtils {
    private FileUtils() {}

    public static boolean isAbsolute(final String path) {
        return path.startsWith("/") || path.matches("^[A-Za-z]:\\\\.*");
    }

    public static String getExtension(Path path) {
        final String name = path.getFileName().toString();
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return "";
        } else {
            return name.substring(index + 1);
        }
    }

    public static String pathWithoutExtension(String path) {
        int i = path.lastIndexOf('.');
        return i == -1 ? path : path.substring(0, i);
    }

    public static void createDirectoriesSafe(Path path) throws IOException {
        Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
    }

    public static void copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        if (buffer.length == 0) {
            throw new IllegalArgumentException("Copy buffer must not be empty");
        }
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    public static boolean contentEquals(Path first, Path second, byte[] buffer) throws IOException {
        int capacity = buffer.length / 2;
        if (capacity == 0) {
            throw new IllegalArgumentException("Comparison buffer needs space for both files");
        }
        try (InputStream left = Files.newInputStream(first); InputStream right = Files.newInputStream(second)) {
            int length;
            do {
                length = left.readNBytes(buffer, 0, capacity);
                int otherLength = right.readNBytes(buffer, capacity, capacity);
                if (length != otherLength || !Arrays.equals(buffer, 0, length, buffer, capacity, capacity + otherLength)) {
                    return false;
                }
            } while (length == capacity);
            return true;
        }
    }

    public static void deleteDirectory(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (Stream<Path> walk = Files.walk(folder, FileVisitOption.FOLLOW_LINKS)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                    });
        }
    }

    public static List<Path> getYmlConfigsDeeply(Path configFolder) {
        if (!Files.exists(configFolder)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(configFolder, FileVisitOption.FOLLOW_LINKS)) {
            return stream.parallel()
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to traverse directory: " + configFolder, e);
        }
    }

    public static List<Path> collectOverlays(Path resourcePackFolder, Predicate<String> directoryPredicate) throws IOException {
        List<Path> folders = new ObjectArrayList<>();
        try (Stream<Path> paths = Files.list(resourcePackFolder)) {
            folders.addAll(paths
                    .filter(Files::isDirectory)
                    .filter(path -> directoryPredicate.test(path.getFileName().toString()))
                    .filter(path -> !path.getFileName().toString().equals("assets"))
                    .filter(path -> Files.exists(path.resolve("assets")))
                    .toList());
        }
        return folders;
    }

    public static List<Path> collectOverlays(Path resourcePackFolder) throws IOException {
        List<Path> folders = new ObjectArrayList<>();
        try (Stream<Path> paths = Files.list(resourcePackFolder)) {
            folders.addAll(paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().equals("assets"))
                    .filter(path -> Files.exists(path.resolve("assets")))
                    .toList());
        }
        return folders;
    }

    public static List<Path> collectNamespaces(Path assetsFolder) throws IOException {
        List<Path> folders;
        try (Stream<Path> paths = Files.list(assetsFolder)) {
            folders = new ObjectArrayList<>(paths
                    .filter(Files::isDirectory)
                    .filter(path -> Identifier.isValidNamespace(path.getFileName().toString()))
                    .toList());
        }
        return folders;
    }

    public static List<Path> getFilesDeeply(Path path) throws IOException {
        List<Path> files = new ObjectArrayList<>();
        Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    public static void deleteEmptyParentDirectories(Path dir, Path stopPath) {
        try {
            if (Files.isDirectory(dir) && isDirectoryEmpty(dir)) {
                Files.delete(dir);
                Path parent = dir.getParent();
                if (parent != null && !parent.equals(stopPath)) {
                    deleteEmptyParentDirectories(parent, stopPath);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static boolean isDirectoryEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static boolean isJsonFile(Path filePath) {
        return filePath.getFileName().toString().endsWith(".json");
    }

    public static boolean isMcMetaFile(Path filePath) {
        return filePath.getFileName().toString().endsWith(".mcmeta");
    }

    public static boolean isPngFile(Path filePath) {
        return filePath.getFileName().toString().endsWith(".png");
    }

    public static boolean isOggFile(Path filePath) {
        return filePath.getFileName().toString().endsWith(".ogg");
    }
}
