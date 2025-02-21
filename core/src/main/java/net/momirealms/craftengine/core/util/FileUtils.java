package net.momirealms.craftengine.core.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FileUtils {

    private FileUtils() {}

    public static void createDirectoriesSafe(Path path) throws IOException {
        Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
    }

    public static Pair<List<Path>, List<Path>> getConfigsDeeply(Path configFolder) {
        if (!Files.exists(configFolder)) return Pair.of(List.of(), List.of());
        List<Path> validYaml = new ArrayList<>();
        List<Path> validJson = new ArrayList<>();
        Deque<Path> pathDeque = new ArrayDeque<>();
        pathDeque.push(configFolder);
        while (!pathDeque.isEmpty()) {
            Path path = pathDeque.pop();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path subPath : stream) {
                    if (Files.isDirectory(subPath)) {
                        pathDeque.push(subPath);
                    } else if (Files.isRegularFile(subPath)) {
                        String pathString = subPath.toString();
                        if (pathString.endsWith(".yml")) {
                            validYaml.add(subPath);
                        } else if (pathString.endsWith(".json")) {
                            validJson.add(subPath);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Pair.of(validYaml, validJson);
    }

    public static List<List<Path>> mergeFolder(Collection<Path> sourceFolders, Path targetFolder) throws IOException {
        Map<Path, List<Path>> conflictChecker = new HashMap<>();
        for (Path sourceFolder : sourceFolders) {
            if (Files.exists(sourceFolder)) {
                Files.walkFileTree(sourceFolder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path targetPath = targetFolder.resolve(sourceFolder.relativize(file));
                        List<Path> conflicts = conflictChecker.computeIfAbsent(targetPath, k -> new ArrayList<>());
                        conflicts.add(file);
                        if (conflicts.size() == 1) {
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        List<List<Path>> conflicts = new ArrayList<>();
        for (Map.Entry<Path, List<Path>> entry : conflictChecker.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(entry.getValue());
            }
        }
        return conflicts;
    }
}
