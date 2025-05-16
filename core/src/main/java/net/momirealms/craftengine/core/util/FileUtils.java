package net.momirealms.craftengine.core.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class FileUtils {

    private FileUtils() {}

    public static String pathWithoutExtension(String path) {
        int i = path.lastIndexOf('.');
        return i == -1 ? path : path.substring(0, i);
    }

    public static void createDirectoriesSafe(Path path) throws IOException {
        Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
    }

    public static List<Path> getYmlConfigsDeeply(Path configFolder) {
        if (!Files.exists(configFolder)) return List.of();
        List<Path> validYaml = new ArrayList<>();
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
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return validYaml;
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
}
