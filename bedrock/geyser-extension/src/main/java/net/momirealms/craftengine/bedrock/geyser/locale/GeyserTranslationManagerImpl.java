package net.momirealms.craftengine.bedrock.geyser.locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.geyser.GeyserProperties;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.sparrow.yaml.SparrowYaml;
import net.momirealms.sparrow.yaml.YamlDocument;
import net.momirealms.sparrow.yaml.libs.snakeyaml.engine.v2.api.Dump;
import net.momirealms.sparrow.yaml.libs.snakeyaml.engine.v2.api.DumpSettings;
import net.momirealms.sparrow.yaml.libs.snakeyaml.engine.v2.api.Load;
import net.momirealms.sparrow.yaml.libs.snakeyaml.engine.v2.api.LoadSettings;
import net.momirealms.sparrow.yaml.libs.snakeyaml.engine.v2.common.FlowStyle;
import net.momirealms.sparrow.yaml.libs.snakeyaml.engine.v2.common.ScalarStyle;
import net.momirealms.sparrow.yaml.node.ScalarNode;
import net.momirealms.sparrow.yaml.node.YamlNode;
import net.momirealms.sparrow.yaml.route.Route;
import org.geysermc.geyser.api.extension.ExtensionLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class GeyserTranslationManagerImpl implements GeyserTranslationManager {
    static GeyserTranslationManager instance;
    private static final Pattern ARGUMENT = Pattern.compile("<arg:(\\d+)>");
    private final Set<String> supportedLanguages;
    private final Path translationsDirectory;
    private final ExtensionLogger logger;
    private final SparrowYaml yaml = SparrowYaml.builder().build();
    private final String languageVersion = GeyserProperties.get("lang-version");
    private final Map<String, String> fallback;
    private Map<String, String> messages;

    public GeyserTranslationManagerImpl(Path directory, ExtensionLogger logger) throws IOException {
        this.translationsDirectory = directory.resolve("translations");
        this.logger = logger;
        this.fallback = this.readBuiltinLanguage("en.yml");
        this.messages = this.fallback;
        this.supportedLanguages = this.getSupportedLanguages();
        instance = this;
    }

    private Set<String> getSupportedLanguages() throws IOException {
        try (InputStream stream = GeyserTranslationManagerImpl.class.getResourceAsStream("/translations/_index.json")) {
            if (stream == null) {
                throw new IOException("Embedded translation index not found");
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject index = JsonParser.parseReader(reader).getAsJsonObject();
                Set<String> languages = new LinkedHashSet<>();
                for (JsonElement file : index.getAsJsonArray("file")) {
                    String name = file.getAsString();
                    languages.add(name.substring(0, name.lastIndexOf('.')));
                }
                return languages;
            }
        }
    }

    @Override
    public void load() {
        Map<String, Map<String, String>> languages = new HashMap<>();
        this.loadBuiltinLanguages(languages);
        this.loadCustomLanguages(languages);
        this.selectLanguage(languages);
    }

    private void loadBuiltinLanguages(Map<String, Map<String, String>> languages) {
        for (String language : this.supportedLanguages) {
            Path path = this.translationsDirectory.resolve(language + ".yml");
            try {
                Map<String, String> translations;
                if (Files.notExists(path)) {
                    translations = this.updateLangFile(Map.of(), path);
                } else {
                    YamlDocument document = this.yaml.load(path);
                    translations = translations(document);
                    if (!this.languageVersion.equals(document.getStringOrDefault("", Route.from("___version___")))) {
                        translations = this.updateLangFile(translations, path);
                    }
                }
                languages.put(language, translations);
            } catch (IOException | RuntimeException exception) {
                GeyserTranslationManager.warn("translation.file_failed", exception, path);
            }
        }
    }

    private Map<String, String> readBuiltinLanguage(String fileName) throws IOException {
        try (InputStream stream = GeyserTranslationManagerImpl.class.getResourceAsStream("/translations/" + fileName)) {
            if (stream == null) {
                throw new IOException("Embedded translation file not found: " + fileName);
            }
            return translations(this.yaml.load(stream));
        }
    }

    private Map<String, String> updateLangFile(Map<String, String> previous, Path path) throws IOException {
        String fileName = path.getFileName().toString();
        LoadSettings loadSettings = LoadSettings.builder().setLabel(fileName).build();
        DumpSettings dumpSettings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .setIndent(2)
                .setIndicatorIndent(2)
                .setSplitLines(false)
                .setDefaultScalarStyle(ScalarStyle.PLAIN)
                .build();
        try (InputStream stream = GeyserTranslationManagerImpl.class.getResourceAsStream("/translations/" + fileName)) {
            if (stream == null) {
                throw new IOException("Embedded translation file not found: " + fileName);
            }
            Load load = new Load(loadSettings);
            @SuppressWarnings("unchecked")
            Map<String, String> defaults = (Map<String, String>) load.loadFromInputStream(stream);
            Map<String, String> contents = new LinkedHashMap<>();
            contents.put("___version___", this.languageVersion);
            contents.putAll(this.fallback);
            if (defaults != null) {
                contents.putAll(defaults);
            }
            contents.putAll(previous);
            Dump dump = new Dump(dumpSettings);
            Files.createDirectories(this.translationsDirectory);
            Files.writeString(path, dump.dumpToString(contents));
            contents.remove("___version___");
            return Map.copyOf(contents);
        } catch (Exception exception) {
            throw new IOException("Error processing YAML for " + fileName, exception);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void loadCustomLanguages(Map<String, Map<String, String>> languages) {
        if (!Files.isDirectory(this.translationsDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(this.translationsDirectory)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString();
                if (!name.endsWith(".yml") || this.supportedLanguages.contains(name.substring(0, name.length() - 4))) {
                    continue;
                }
                try {
                    languages.put(normalize(name.substring(0, name.length() - 4)), translations(this.yaml.load(path)));
                } catch (IOException | RuntimeException exception) {
                    GeyserTranslationManager.warn("translation.file_failed", exception, path);
                }
            }
        } catch (IOException exception) {
            GeyserTranslationManager.warn("translation.file_failed", exception, this.translationsDirectory);
        }
    }

    private void selectLanguage(Map<String, Map<String, String>> languages) {
        if (languages.containsKey("zh_cn")) {
            languages.putIfAbsent("zh", languages.get("zh_cn"));
        }
        String forcedLocale = GeyserConfig.forcedLocale();
        String locale = normalize(forcedLocale.isBlank() ? Locale.getDefault().toString() : forcedLocale);
        String language = locale.split("_", 2)[0];
        Map<String, String> selected = new HashMap<>(this.fallback);
        selected.putAll(languages.getOrDefault(language, Map.of()));
        selected.putAll(languages.getOrDefault(locale, Map.of()));
        this.messages = Map.copyOf(selected);
    }

    private static String normalize(String locale) {
        return locale.trim().replace('-', '_').toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> translations(YamlDocument document) throws IOException {
        Map<String, String> translations = new HashMap<>();
        for (Map.Entry<Object, YamlNode<?>> entry : document.value().entrySet()) {
            String key = entry.getKey().toString();
            if (key.equals("___version___")) {
                continue;
            }
            if (!(entry.getValue() instanceof ScalarNode scalar) || !(scalar.value() instanceof String message)) {
                throw new IOException("Expected a text translation for " + key);
            }
            translations.put(key, message);
        }
        return Map.copyOf(translations);
    }

    @Override
    public String plainTranslation(String key, Object... arguments) {
        String template = this.messages.getOrDefault(key, key);
        if (arguments.length == 0) {
            return template;
        }
        Matcher matcher = ARGUMENT.matcher(template);
        StringBuilder result = new StringBuilder(template.length());
        while (matcher.find()) {
            int index;
            try {
                index = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException exception) {
                continue;
            }
            if (index < arguments.length) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(arguments[index])));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public ExtensionLogger logger() {
        return this.logger;
    }
}
