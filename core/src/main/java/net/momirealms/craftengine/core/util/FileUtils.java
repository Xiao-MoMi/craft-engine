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

package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
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

    public static List<Path> getAllFiles(Path path) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    public static void moveFile(Path sourcePath, Path targetPath, boolean moveMcmeta) throws IOException {
        if (!Files.exists(sourcePath)) {
            throw new FileNotFoundException("Source file does not exist: " + sourcePath);
        }
        Path targetParent = targetPath.getParent();
        if (targetParent != null) {
            Files.createDirectories(targetParent);
        }
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        if (moveMcmeta) {
            Path mcmetaSource = getMcmetaPath(sourcePath);
            Path mcmetaTarget = getMcmetaPath(targetPath);
            Files.move(mcmetaSource, mcmetaTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Path getMcmetaPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".mcmeta");
    }

    public static void deleteEmptyDirectories(Path rootPath) throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (isDirectoryEmpty(dir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isDirectoryEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static boolean inNamespaceFolder(Path path, Path rootPath) {
        path = path.toAbsolutePath();
        rootPath = rootPath.toAbsolutePath();
        int count = rootPath.relativize(path).getNameCount();
        return count >= 3;
    }
}
