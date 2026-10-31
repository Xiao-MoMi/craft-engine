package net.momirealms.craftengine.bedrock.geyser.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MinecraftLocaleProxy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class GeyserTranslations {

    private GeyserTranslations() {
    }

    public static Map<String, Map<String, String>> read(List<Path> packs) throws IOException {
        Map<String, Map<String, String>> translations = new HashMap<>();
        for (Path pack : packs) {
            for (Map.Entry<String, Map<String, String>> entry : readPack(pack).entrySet()) {
                translations.computeIfAbsent(entry.getKey(), locale -> new HashMap<>()).putAll(entry.getValue());
            }
        }
        return translations;
    }

    public static void install(Supplier<GeyserTextData> source, Path configDirectory) throws IOException {
        MinecraftLocaleProxy proxy = MinecraftLocaleProxy.INSTANCE;
        proxy.localeMappings(new GeyserTranslationMap(
                proxy.localeMappings(),
                source,
                readOverrides(configDirectory.resolve("locales/overrides"))
        ));
    }

    private static Map<String, Map<String, String>> readPack(Path path) throws IOException {
        try (ZipFile archive = new ZipFile(path.toFile())) {
            ZipEntry entry = archive.getEntry(BedrockPackLayout.TRANSLATIONS);
            if (entry == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Map<String, String>> translations = new HashMap<>();
                for (Map.Entry<String, JsonElement> language : json.entrySet()) {
                    translations.computeIfAbsent(normalizeLocale(language.getKey()), locale -> new HashMap<>())
                            .putAll(readLanguage(language.getValue().getAsJsonObject()));
                }
                return translations;
            }
        }
    }

    private static Map<String, Map<String, String>> readOverrides(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return Map.of();
        }
        Map<String, Map<String, String>> overrides = new HashMap<>();
        try (Stream<Path> paths = Files.list(directory)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            for (Path file : files) {
                String name = file.getFileName().toString();
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    overrides.put(normalizeLocale(name.substring(0, name.length() - 5)),
                            readLanguage(JsonParser.parseReader(reader).getAsJsonObject()));
                }
            }
        }
        return overrides;
    }

    private static Map<String, String> readLanguage(JsonObject json) {
        Map<String, String> translations = new HashMap<>(json.size());
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            translations.put(entry.getKey(), entry.getValue().getAsString());
        }
        return translations;
    }

    private static String normalizeLocale(String locale) {
        String normalized = locale.toLowerCase(Locale.ROOT);
        return normalized.equals("no_no") ? "nb_no" : normalized;
    }
}
