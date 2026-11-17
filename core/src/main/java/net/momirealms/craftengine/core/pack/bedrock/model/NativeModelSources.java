package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class NativeModelSources {

    private NativeModelSources() {
    }

    public static List<NativeModelSource> collect(String namespace, Path directory, Collection<String> loaded) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        Map<String, Path> files = new HashMap<>();
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString();
                if (!name.endsWith(".bbmodel")) {
                    continue;
                }
                String stem = name.substring(0, name.length() - 8).toLowerCase(Locale.ROOT);
                if (files.putIfAbsent(stem, path) != null) {
                    throw new IOException("Ambiguous native model blueprint: " + stem + " in " + directory);
                }
            }
        }
        List<NativeModelSource> result = new ArrayList<>();
        for (String name : loaded.stream().sorted().toList()) {
            Path file = files.get(name.toLowerCase(Locale.ROOT));
            if (file != null) {
                result.add(new NativeModelSource(Key.of(namespace, name), file));
            }
        }
        return result;
    }
}
