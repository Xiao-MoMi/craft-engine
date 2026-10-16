package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

final class BedrockLanguageWriter {
    private static final Pattern NUMERIC_FORMAT = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    private static final Set<String> BEDROCK_LOCALES = Set.of("en_US", "en_GB", "de_DE", "es_ES", "es_MX", "fr_FR", "fr_CA",
            "it_IT", "ja_JP", "ko_KR", "pt_BR", "pt_PT", "ru_RU", "zh_CN", "zh_TW", "nl_NL", "bg_BG", "cs_CZ", "da_DK",
            "el_GR", "fi_FI", "hu_HU", "id_ID", "nb_NO", "pl_PL", "sk_SK", "sv_SE", "tr_TR", "uk_UA");

    private BedrockLanguageWriter() {
    }

    static void write(JavaResourcePack pack, Path root, Consumer<String> warning) throws IOException {
        Map<String, Map<String, String>> locales = new TreeMap<>();
        for (String path : pack.languageResources()) {
            String locale = bedrockLocale(path.substring(path.lastIndexOf('/') + 1, path.length() - 5));
            if (locale == null) {
                continue;
            }
            JsonObject json = pack.readJson(path);
            Map<String, String> strings = locales.computeIfAbsent(locale, ignored -> new TreeMap<>());
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    warning.accept("Skipping non-primitive language entry " + entry.getKey() + " in " + path);
                    continue;
                }
                // Java language loading turns numeric printf specifiers into string substitutions.
                String value = NUMERIC_FORMAT.matcher(entry.getValue().getAsString()).replaceAll("%$1s");
                strings.put(entry.getKey(), value);
            }
        }
        JsonArray languages = new JsonArray();
        Path texts = root.resolve("texts");
        Files.createDirectories(texts);
        for (Map.Entry<String, Map<String, String>> locale : locales.entrySet()) {
            String language = locale.getKey();
            languages.add(language);
            try (BufferedWriter writer = Files.newBufferedWriter(texts.resolve(language + ".lang"), StandardCharsets.UTF_8)) {
                for (Map.Entry<String, String> entry : locale.getValue().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key.indexOf('=') >= 0 || key.indexOf('#') >= 0 || key.isBlank() || !singleLine(key) || !singleLine(value) || value.contains("##")) {
                        warning.accept("Skipping language entry " + key + " (" + language + "): cannot be represented in .lang");
                        continue;
                    }
                    writer.write(key);
                    writer.write('=');
                    writer.write(value);
                    writer.write("\t#\n");
                }
            }
        }
        GsonHelper.writeJsonFile(languages, texts.resolve("languages.json"));
    }

    private static String bedrockLocale(String javaLocale) {
        if (javaLocale.equals("no_no")) {
            return "nb_NO";
        }
        int separator = javaLocale.indexOf('_');
        String locale = javaLocale.substring(0, separator + 1) + javaLocale.substring(separator + 1).toUpperCase(Locale.ROOT);
        return BEDROCK_LOCALES.contains(locale) ? locale : null;
    }

    private static boolean singleLine(String value) {
        return value.indexOf('\r') < 0 && value.indexOf('\n') < 0 && value.indexOf('\t') < 0;
    }
}
