package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.config.StringKeyConstructor;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TranslationManagerImpl implements TranslationManager {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    protected static TranslationManager instance;
    private final Plugin plugin;
    private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
    private final Path translationsDirectory;
    private final Map<Locale, I18NData> i18nData = new HashMap<>();
    private final ClientLangManager clientLangManager;
    private final String langVersion;
    private final String[] supportedLanguages;
    private final Map<String, Object> translationFallback = new LinkedHashMap<>();
    private Locale forcedLocale = null;
    private Locale selectedLocale = DEFAULT_LOCALE;
    private MiniMessageTranslationRegistry registry;

    public TranslationManagerImpl(Plugin plugin) {
        this.plugin = plugin;
        this.translationsDirectory = this.plugin.dataFolderPath().resolve("translations");
        this.clientLangManager = new ClientLangMangerImpl(plugin);
        this.langVersion = PluginProperties.getValue("lang-version");
        this.supportedLanguages = PluginProperties.getValue("supported-languages").split(",");
        instance = this;

        Yaml yaml = new Yaml(new StringKeyConstructor(new LoaderOptions()));
        try (InputStream is = plugin.resourceStream("translations/en.yml")) {
            this.translationFallback.putAll(yaml.load(is));
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to load default translation file", e);
        }
    }

    @Override
    public void forcedLocale(Locale locale) {
        this.forcedLocale = locale;
    }

    @Override
    public void reload() {
        // clear old data
        this.i18nData.clear();
        this.clientLangManager.reload();

        // remove any previous registry
        if (this.registry != null) {
            MiniMessageTranslator.translator().removeSource(this.registry);
            this.installed.clear();
        }

        // save resources
        for (String lang : this.supportedLanguages) {
            this.plugin.saveResource("translations/" + lang + ".yml");
        }

        this.registry = MiniMessageTranslationRegistry.create(Key.key("craftengine", "main"), AdventureHelper.miniMessage());
        this.registry.defaultLocale(DEFAULT_LOCALE);
        this.loadFromFileSystem(this.translationsDirectory, false);
        MiniMessageTranslator.translator().addSource(this.registry);
        this.setSelectedLocale();
    }

    private void setSelectedLocale() {
        if (this.forcedLocale != null) {
            this.selectedLocale = forcedLocale;
            if (!this.installed.contains(forcedLocale)) {
                this.plugin.logger().warn("The forced locale is set to " + forcedLocale + ", but it is not available.");
            }
            return;
        }

        Locale localLocale = Locale.getDefault();
        if (this.installed.contains(localLocale)) {
            this.selectedLocale = localLocale;
            return;
        }

        Locale langLocale = Locale.of(localLocale.getLanguage());
        if (this.installed.contains(langLocale)) {
            this.selectedLocale = langLocale;
            return;
        }

        this.plugin.logger().warn(localLocale.toString().toLowerCase(Locale.ENGLISH) + ".yml not exists, using " + DEFAULT_LOCALE.toString().toLowerCase(Locale.ENGLISH) + ".yml as default locale.");
        this.selectedLocale = DEFAULT_LOCALE;
    }

    @Override
    public String miniMessageTranslation(String key, @Nullable Locale locale) {
        if (locale == null) {
            locale = this.selectedLocale;
        }
        return this.registry.miniMessageTranslation(key, locale);
    }

    @Override
    public Component render(Component component, @Nullable Locale locale) {
        if (locale == null) {
            locale = this.selectedLocale;
        }
        return MiniMessageTranslator.render(component, locale);
    }

    public void loadFromFileSystem(Path directory, boolean suppressDuplicatesError) {
        List<Path> translationFiles;
        try (Stream<Path> stream = Files.list(directory)) {
            translationFiles = stream.filter(TranslationManagerImpl::isTranslationFile).collect(Collectors.toList());
        } catch (IOException e) {
            translationFiles = Collections.emptyList();
        }

        if (translationFiles.isEmpty()) {
            return;
        }

        Map<Locale, Map<String, String>> loaded = new HashMap<>();
        for (Path translationFile : translationFiles) {
            try {
                Pair<Locale, Map<String, String>> result = loadTranslationFile(translationFile);
                loaded.put(result.left(), result.right());
            } catch (Exception e) {
                if (!suppressDuplicatesError || !isAdventureDuplicatesException(e)) {
                    this.plugin.logger().warn("Error loading locale file: " + translationFile.getFileName(), e);
                }
            }
        }

        // try registering the locale without a country code - if we don't already have a registration for that
        loaded.forEach((locale, bundle) -> {
            Locale localeWithoutCountry = Locale.of(locale.getLanguage());
            if (!locale.equals(localeWithoutCountry) && !localeWithoutCountry.equals(DEFAULT_LOCALE) && this.installed.add(localeWithoutCountry)) {
                try {
                    this.registry.registerAll(localeWithoutCountry, bundle);
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        });
    }

    public static boolean isTranslationFile(Path path) {
        return path.getFileName().toString().endsWith(".yml");
    }

    private static boolean isAdventureDuplicatesException(Exception e) {
        return e instanceof IllegalArgumentException && (e.getMessage().startsWith("Invalid key") || e.getMessage().startsWith("Translation already exists"));
    }

    @SuppressWarnings("unchecked")
    private Pair<Locale, Map<String, String>> loadTranslationFile(Path translationFile) {
        String fileName = translationFile.getFileName().toString();
        String localeString = fileName.substring(0, fileName.length() - ".yml".length());
        Locale locale = TranslationManager.parseLocale(localeString);
        if (locale == null) {
            throw new IllegalStateException("Unknown locale '" + localeString + "' - unable to register.");
        }

        Map<String, String> bundle = new HashMap<>();
        Yaml yaml = new Yaml(new StringKeyConstructor(new LoaderOptions()));
        try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(translationFile.toFile()), StandardCharsets.UTF_8)) {
            Map<String, Object> map = yaml.load(inputStream);
            String langVersion = map.getOrDefault("lang-version", "").toString();
            if (!langVersion.equals(this.langVersion)) {
                map = updateLangFile(map, translationFile);
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String str) {
                    bundle.put(entry.getKey(), str);
                } else if (entry.getValue() instanceof List<?> list) {
                    List<String> strList = (List<String>) list;
                    StringJoiner stringJoiner = new StringJoiner("<reset><newline>");
                    for (String str : strList) {
                        stringJoiner.add(str);
                    }
                    bundle.put(entry.getKey(), stringJoiner.toString());
                }
            }

            this.registry.registerAll(locale, bundle);
            this.installed.add(locale);
        } catch (IOException e) {
            this.plugin.logger().warn(translationFile, "Error loading translation file", e);
        }

        return Pair.of(locale, bundle);
    }

    @Override
    public String translateI18NTag(String i18nId) {
        I18NData data = this.i18nData.get(this.selectedLocale);
        String translation = getI18NOrNull(data, i18nId);
        if (translation != null) return translation;
        Locale lang = Locale.of(selectedLocale.getLanguage());
        if (!this.selectedLocale.equals(lang)) {
            data = this.i18nData.get(lang);
            translation = getI18NOrNull(data, i18nId);
            if (translation != null) return translation;
        }
        I18NData fallback = this.i18nData.get(DEFAULT_LOCALE);
        if (fallback != null) {
            translation = fallback.translate(i18nId);
            return translation == null ? i18nId : translation;
        }
        return i18nId;
    }

    @Nullable
    private String getI18NOrNull(I18NData data, String i18nId) {
        if (data == null) return null;
        return data.translate(i18nId);
    }

    @Override
    public void parseSection(Pack pack, Path path, net.momirealms.craftengine.core.util.Key id, Map<String, Object> section) {
        Locale locale = TranslationManager.parseLocale(id.value());
        if (locale == null) {
            throw new IllegalStateException("Unknown locale '" + id.value() + "' - unable to register.");
        }

        I18NData data = this.i18nData.computeIfAbsent(locale, k -> new I18NData());
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey();
            data.addTranslation(key, entry.getValue().toString());
        }
    }

    @Override
    public ClientLangManager clientLangManager() {
        return clientLangManager;
    }

    private Map<String, Object> updateLangFile(Map<String, Object> previous, Path translationFile) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setSplitLines(false);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        Yaml yaml = new Yaml(new StringKeyConstructor(new LoaderOptions()), new Representer(options), options);
        LinkedHashMap<String, Object> newFileContents = new LinkedHashMap<>();
        try (InputStream is = plugin.resourceStream("translations/" + translationFile.getFileName())) {
            Map<String, Object> newMap = yaml.load(is);
            newFileContents.putAll(this.translationFallback);
            newFileContents.putAll(newMap);
            newFileContents.putAll(previous);
            newFileContents.put("lang-version", this.langVersion);
            String yamlString = yaml.dump(newFileContents);
            Files.writeString(translationFile, yamlString);
            return newFileContents;
        }
    }
}
