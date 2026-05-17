package net.momirealms.craftengine.core.plugin.locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.config.yaml.TranslationConfigConstructor;
import net.momirealms.craftengine.core.plugin.text.minimessage.ImageTag;
import net.momirealms.craftengine.core.plugin.text.minimessage.IndexedArgumentTag;
import net.momirealms.craftengine.core.plugin.text.minimessage.ShiftTag;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TranslationManagerImpl implements TranslationManager {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    static TranslationManager instance;
    private final Plugin plugin;
    private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
    private final Path translationsDirectory;
    private final String langVersion;
    private final Set<String> supportedLanguages;
    private final Map<String, String> translationFallback = new LinkedHashMap<>();
    private Locale selectedLocale = DEFAULT_LOCALE;
    private final Map<String, ClientLangData> clientLangData = new HashMap<>();
    private final Map<String, ServerLangData> serverLangData = new HashMap<>();
    private final LangParser langParser;
    private final TranslationParser translationParser;
    private final Set<String> allLang;
    private final List<Suggestion> allLangSuggestions;
    private final Map<String, List<String>> locale2Countries;
    private Map<Locale, CachedTranslation> cachedTranslations = Map.of();

    public TranslationManagerImpl(Plugin plugin) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = this;
        this.plugin = plugin;
        this.translationsDirectory = this.plugin.dataFolderPath().resolve("translations");
        this.langVersion = PluginProperties.getValue("lang-version");
        this.supportedLanguages = getSupportedLanguages();
        this.langParser = new LangParser();
        this.translationParser = new TranslationParser();
        try (InputStream is = plugin.resourceStream("translations/en.yml")) {
            LoadSettings settings = LoadSettings.builder().setLabel("translations/en.yml").build();
            TranslationConfigConstructor constructor = new TranslationConfigConstructor(settings);
            Load load = new Load(settings, constructor);
            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) load.loadFromInputStream(is);
            if (data != null) {
                this.translationFallback.putAll(data);
            }
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to load default translation file", e);
        } catch (Exception e) {
            CraftEngine.instance().logger().error("YAML syntax error in default translation file", e);
        }
        Set<String> allLang = new HashSet<>();
        try (InputStream inputStream = CraftEngine.instance().resourceStream("internal/lang/processed.json")) {
            Objects.requireNonNull(inputStream);
            JsonArray listJson = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : listJson) {
                allLang.add(element.getAsString());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load internal/lang/processed.json", e);
        }
        this.allLang = Collections.unmodifiableSet(allLang);
        this.allLangSuggestions = this.allLang.stream().map(Suggestion::suggestion).toList();
        this.locale2Countries = this.allLang.stream()
                .map(lang -> lang.split("_"))
                .filter(split -> split.length >= 2)
                .collect(Collectors.groupingBy(
                        split -> split[0],
                        Collectors.mapping(split -> split[1], Collectors.toUnmodifiableList())
                ));
    }

    private Set<String> getSupportedLanguages() {
        InputStream stream = this.plugin.resourceStream("translations/_index.json");
        if (stream == null) return Set.of();
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = GsonHelper.get().fromJson(reader, JsonObject.class);
            Set<String> supportedLanguages = new HashSet<>();
            for (JsonElement file : json.getAsJsonArray("file")) {
                supportedLanguages.add(FileUtils.pathWithoutExtension(file.getAsString()));
            }
            return supportedLanguages;
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to load default translation file", e);
            return Set.of();
        }
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.langParser, this.translationParser};
    }

    @Override
    public void delayedLoad() {
        this.clientLangData.values().forEach(ClientLangData::processTranslations);
    }

    @Override
    public void reload() {
        // clear old data
        this.clientLangData.clear();
        this.serverLangData.clear();
        this.installed.clear();

        // save resources
        for (String lang : this.supportedLanguages) {
            this.plugin.saveResource("translations/" + lang + ".yml");
        }

        this.loadFromFileSystem(this.translationsDirectory);
        this.loadFromCache();
        this.setSelectedLocale();
    }

    private void setSelectedLocale() {
        if (Config.forcedLocale() != null) {
            this.selectedLocale = Config.forcedLocale();
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

        this.plugin.logger().warn("translations/" + localLocale.toString().toLowerCase(Locale.ENGLISH) + ".yml not exists, using " + DEFAULT_LOCALE.toString().toLowerCase(Locale.ENGLISH) + ".yml as default locale.");
        this.selectedLocale = DEFAULT_LOCALE;
    }

    @Override
    public String miniMessageTranslation(String key, @Nullable Locale locale) {
        ServerLangData serverLangData = this.serverLangData.get(key);
        if (serverLangData == null) {
            return key;
        }
        if (locale == null) {
            locale = this.selectedLocale;
        }
        return Optional.ofNullable(serverLangData.translate(locale)).orElse(key);
    }

    @Override
    public Component render(TranslatableComponent component, @Nullable Locale locale) {
        String miniMessageTranslation = miniMessageTranslation(component.key(), locale);
        if (miniMessageTranslation == null) {
            return component;
        }
        if (miniMessageTranslation.isEmpty()) {
            return Component.empty();
        }
        final Component resultingComponent;
        if (component.arguments().isEmpty()) {
            resultingComponent = AdventureHelper.miniMessage().deserialize(miniMessageTranslation, ShiftTag.INSTANCE, ImageTag.INSTANCE);
        } else {
            resultingComponent = AdventureHelper.miniMessage().deserialize(miniMessageTranslation, new IndexedArgumentTag(component.arguments()), ShiftTag.INSTANCE, ImageTag.INSTANCE);
        }
        if (component.children().isEmpty()) {
            return resultingComponent;
        } else {
            return resultingComponent.children(component.children());
        }
    }

    @Override
    public Set<String> translationKeys() {
        return this.serverLangData.keySet();
    }

    private void loadFromCache() {
        // 第一阶段：先注册所有没有国家/地区的locale
        for (Map.Entry<Locale, CachedTranslation> entry : this.cachedTranslations.entrySet()) {
            Locale locale = entry.getKey();
            // 只处理没有国家/地区的locale
            if (locale.getCountry().isEmpty()) {
                registerAll(locale, entry.getValue().translations);
            }
        }

        // 第二阶段：再注册其他完整的locale（包含国家/地区）
        for (Map.Entry<Locale, CachedTranslation> entry : this.cachedTranslations.entrySet()) {
            Locale locale = entry.getKey();
            // 跳过已经注册的无国家locale
            if (!locale.getCountry().isEmpty()) {
                registerAll(locale, entry.getValue().translations);

                // 如果需要，为有国家/地区的locale也注册无国家版本，可以提升一定的兼容性
                Locale localeWithoutCountry = Locale.of(locale.getLanguage());
                if (!this.installed.contains(localeWithoutCountry) && !localeWithoutCountry.equals(DEFAULT_LOCALE)) {
                    registerAll(localeWithoutCountry, entry.getValue().translations);
                }
            }
        }
    }

    private void registerAll(Locale locale, Map<String, String> cachedTranslation) {
        for (Map.Entry<String, String> translation : cachedTranslation.entrySet()) {
            this.serverLangData.computeIfAbsent(translation.getKey(), k -> new ServerLangData(this.translationFallback.get(translation.getKey())))
                    .addTranslation(locale, translation.getValue());
        }
        this.installed.add(locale);
    }

    public void loadFromFileSystem(Path directory) {
        Map<Locale, CachedTranslation> previousTranslations = this.cachedTranslations;
        this.cachedTranslations = new HashMap<>();
        try {
            Files.walkFileTree(directory, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) {
                    String fileName = path.getFileName().toString();
                    if (Files.isRegularFile(path) && fileName.endsWith(".yml")) {
                        String localeName = fileName.substring(0, fileName.length() - ".yml".length());
                        Locale locale = TranslationManager.parseLocale(localeName);
                        if (locale == null) {
                            TranslationManagerImpl.this.plugin.logger().warn("Invalid translation file " + path);
                            return FileVisitResult.CONTINUE;
                        }
                        CachedTranslation cachedFile = previousTranslations.get(locale);
                        long lastModifiedTime = attrs.lastModifiedTime().toMillis();
                        long size = attrs.size();
                        if (cachedFile != null && cachedFile.lastModified() == lastModifiedTime && cachedFile.size() == size) {
                            TranslationManagerImpl.this.cachedTranslations.put(locale, cachedFile);
                        } else {
                            try (InputStream inputStream = Files.newInputStream(path)) {
                                LoadSettings settings = LoadSettings.builder().setLabel(path.toAbsolutePath().toString()).build();
                                TranslationConfigConstructor constructor = new TranslationConfigConstructor(settings);
                                Load load = new Load(settings, constructor);
                                @SuppressWarnings("unchecked")
                                Map<String, String> data = (Map<String, String>) load.loadFromInputStream(inputStream);
                                if (data == null) return FileVisitResult.CONTINUE;
                                String langVersion = data.getOrDefault("lang-version", "");
                                if (!langVersion.equals(TranslationManagerImpl.this.langVersion) && TranslationManagerImpl.this.supportedLanguages.contains(localeName)) {
                                    data = updateLangFile(data, path);
                                }
                                cachedFile = new CachedTranslation(data, lastModifiedTime, size);
                                TranslationManagerImpl.this.cachedTranslations.put(locale, cachedFile);
                            } catch (IOException e) {
                                TranslationManagerImpl.this.plugin.logger().error("Error while reading translation file: " + path, e);
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to load translation file from folder", e);
        }
    }

    private Map<String, String> updateLangFile(Map<String, String> previous, Path translationFile) throws IOException {
        String fileName = translationFile.getFileName().toString();

        LoadSettings loadSettings = LoadSettings.builder()
                .setLabel(fileName)
                .build();

        DumpSettings dumpSettings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .setIndent(2)
                .setIndicatorIndent(2)
                .setSplitLines(false)
                .setDefaultScalarStyle(ScalarStyle.PLAIN)
                .build();

        LinkedHashMap<String, String> newFileContents = new LinkedHashMap<>();

        try (InputStream is = this.plugin.resourceStream("translations/" + fileName)) {
            TranslationConfigConstructor constructor = new TranslationConfigConstructor(loadSettings);
            Load load = new Load(loadSettings, constructor);

            @SuppressWarnings("unchecked")
            Map<String, String> newMap = (Map<String, String>) load.loadFromInputStream(is);

            previous.remove("lang-version");
            newFileContents.put("lang-version", this.langVersion);
            newFileContents.putAll(this.translationFallback);
            if (newMap != null) {
                newFileContents.putAll(newMap);
            }
            newFileContents.putAll(previous);

            Dump dump = new Dump(dumpSettings);
            String yamlString = dump.dumpToString(newFileContents);

            Files.writeString(translationFile, yamlString);

            return newFileContents;
        } catch (Exception e) {
            throw new IOException("Error processing YAML for " + fileName, e);
        }
    }

    @Override
    public Map<String, ClientLangData> clientLangData() {
        return Collections.unmodifiableMap(this.clientLangData);
    }

    @Override
    public void addClientTranslation(String langId, Map<String, String> translations) {
        if ("all".equals(langId)) {
            this.allLang.forEach(lang -> this.clientLangData.computeIfAbsent(lang, k -> new ClientLangData())
                    .addTranslations(translations));
            return;
        }

        if (this.allLang.contains(langId)) {
            this.clientLangData.computeIfAbsent(langId, k -> new ClientLangData())
                    .addTranslations(translations);
            return;
        }

        List<String> langCountries = this.locale2Countries.getOrDefault(langId, Collections.emptyList());
        for (String lang : langCountries) {
            this.clientLangData.computeIfAbsent(langId + "_" + lang, k -> new ClientLangData())
                    .addTranslations(translations);
        }
    }

    @Override
    public Set<String> allLang() {
        return this.allLang;
    }

    @Override
    public List<Suggestion> allLangSuggestions() {
        return this.allLangSuggestions;
    }

    @Override
    public Map<String, List<String>> locale2Countries() {
        return this.locale2Countries;
    }

    // 为了解决如下的格式兼容 a.b.c
    // a:
    //  b:
    //   c: xxx
    private static void loadLangKeyDeeply(@Nullable String prefix, ConfigSection section, BiConsumer<String, String> collector) {
        for (String key : section.keySet()) {
            ConfigValue value = section.getValue(key);
            if (value == null) continue;
            if (value.is(Map.class)) {
                loadLangKeyDeeply(assembleLangKey(prefix, key), value.getAsSection(), collector);
            } else {
                collector.accept(assembleLangKey(prefix, key), value.getAsString());
            }
        }
    }

    private static String assembleLangKey(@Nullable String prefix, String lang) {
        if (prefix == null) {
            return lang;
        }
        return prefix + "." + lang;
    }

    private final class TranslationParser extends SectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"translations", "translation", "l10n", "localization", "i18n", "internationalization"};
        private final Map<Locale, List<Map<String, String>>> withoutCountry = new HashMap<>();
        private final Map<Locale, List<Map<String, String>>> withCountry = new HashMap<>();
        private int count;

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public void preProcess() {
            this.count = 0;
            this.withoutCountry.clear();
            this.withCountry.clear();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.TRANSLATION;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        protected void parseSection(Pack pack, Path path, ConfigSection section) {
            for (String langId : section.keySet()) {
                Locale locale = TranslationManager.parseLocale(langId);
                if (locale == null) {
                    super.errorHandler.accept(new KnownResourceException("resource.lang.unknown_locale", section.path(), langId));
                    continue;
                }
                ConfigSection dataSection = section.getNonNullSection(langId);
                Map<String, String> bundle = new HashMap<>();
                loadLangKeyDeeply(null, dataSection, bundle::put);
                this.count += bundle.size();
                if (locale.getCountry().isEmpty()) {
                    this.withoutCountry.computeIfAbsent(locale, k -> new ArrayList<>()).add(bundle);
                } else {
                    this.withCountry.computeIfAbsent(locale, k -> new ArrayList<>()).add(bundle);
                }
            }
        }

        @Override
        public void postProcess() {
            for (Map.Entry<Locale, List<Map<String, String>>> entry : this.withoutCountry.entrySet()) {
                for (Map<String, String> bundle : entry.getValue()) {
                    registerAll(entry.getKey(), bundle);
                }
            }
            for (Map.Entry<Locale, List<Map<String, String>>> entry : this.withCountry.entrySet()) {
                Locale locale = entry.getKey();
                Locale withoutCountry = Locale.of(locale.getLanguage());
                for (Map<String, String> bundle : entry.getValue()) {
                    registerAll(locale, bundle);
                    registerAll(withoutCountry, bundle);
                }
            }
        }
    }

    private final class LangParser extends SectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"lang", "language", "languages"};
        private static final Function<String, String> LANG_FORMATTER = s -> {
            Component deserialize = AdventureHelper.miniMessage().deserialize(AdventureHelper.legacyToMiniMessage(s), ShiftTag.INSTANCE, ImageTag.INSTANCE);
            return AdventureHelper.getLegacy().serialize(deserialize);
        };
        private int count;

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public void preProcess() {
            this.count = 0;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.LANG;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.IMAGE);
        }

        @Override
        protected void parseSection(Pack pack, Path path, ConfigSection section) {
            for (String langId : section.keySet()) {
                ConfigSection langSection = section.getNonNullSection(langId);
                Map<String, String> sectionData = new HashMap<>();
                loadLangKeyDeeply(null, langSection, (key, value) -> sectionData.put(key, LANG_FORMATTER.apply(value)));
                this.count += sectionData.size();
                TranslationManagerImpl.this.addClientTranslation(langId, sectionData);
            }
        }
    }

    private record CachedTranslation(Map<String, String> translations, long lastModified, long size) {
    }
}
