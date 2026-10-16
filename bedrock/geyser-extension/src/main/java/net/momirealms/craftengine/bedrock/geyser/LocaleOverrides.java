package net.momirealms.craftengine.bedrock.geyser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import net.momirealms.craftengine.bedrock.BedrockPackLanguages;
import net.momirealms.craftengine.bedrock.BedrockPackLanguagesCodec;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class LocaleOverrides {
    private static final Gson GSON = new Gson();

    private LocaleOverrides() {
    }

    static Set<String> sync(Path directory, Path state, Map<String, Map<String, String>> translations) throws IOException {
        BedrockPackLanguages previous = BedrockPackLanguages.EMPTY;
        if (Files.exists(state)) {
            try (Reader reader = Files.newBufferedReader(state, StandardCharsets.UTF_8)) {
                previous = BedrockPackLanguagesCodec.read(reader);
            }
        }
        Set<String> locales = new TreeSet<>(previous.locales().keySet());
        locales.addAll(translations.keySet());
        Map<String, Map<String, String>> owned = new TreeMap<>();
        Files.createDirectories(directory);
        for (String locale : locales) {
            Path file = directory.resolve(locale + ".json");
            JsonObject original = readOverrides(file);
            JsonObject updated = original.deepCopy();
            // Remove only values we installed; manual additions and edits take precedence.
            for (Map.Entry<String, String> entry : previous.locales().getOrDefault(locale, Map.of()).entrySet()) {
                JsonElement current = updated.get(entry.getKey());
                if (current != null && current.isJsonPrimitive() && entry.getValue().equals(current.getAsString())) {
                    updated.remove(entry.getKey());
                }
            }
            Map<String, String> installed = new TreeMap<>();
            for (Map.Entry<String, String> entry : translations.getOrDefault(locale, Map.of()).entrySet()) {
                if (!updated.has(entry.getKey())) {
                    updated.addProperty(entry.getKey(), entry.getValue());
                    installed.put(entry.getKey(), entry.getValue());
                }
            }
            if (!installed.isEmpty()) {
                owned.put(locale, installed);
            }
            if (!original.equals(updated)) {
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(updated, writer);
                }
            }
        }
        Files.createDirectories(state.getParent());
        try (Writer writer = Files.newBufferedWriter(state, StandardCharsets.UTF_8)) {
            BedrockPackLanguagesCodec.write(new BedrockPackLanguages(owned, Map.of()), writer);
        }
        return locales;
    }

    private static JsonObject readOverrides(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IOException("Invalid Geyser language overrides in " + file, exception);
        }
    }
}
