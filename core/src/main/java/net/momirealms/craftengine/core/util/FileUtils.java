package net.momirealms.craftengine.core.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FileUtils {

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
