package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.text.minimessage.ImageTag;
import net.momirealms.craftengine.core.plugin.text.minimessage.IndexedArgumentTag;
import net.momirealms.craftengine.core.plugin.text.minimessage.ShiftTag;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.ExceptionCollector;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

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

// todo 此类需要重构，统一化管理 客户端资源包语言 与 服务端语言。并进行冲突检测
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
    private MiniMessageTranslationRegistry registry;
    private final Map<String, LangData> clientLangData = new HashMap<>();
    private final LangParser langParser;
    private final TranslationParser translationParser;
    private final Set<String> translationKeys = new HashSet<>();
    private Map<Locale, CachedTranslation> cachedTranslations = Map.of();

    public TranslationManagerImpl(Plugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.translationsDirectory = this.plugin.dataFolderPath().resolve("translations");
        this.langVersion = PluginProperties.getValue("lang-version");
        this.supportedLanguages = Arrays.stream(PluginProperties.getValue("supported-languages").split(",")).collect(Collectors.toSet());
        this.langParser = new LangParser();
        this.translationParser = new TranslationParser();
        Yaml yaml = new Yaml(new TranslationConfigConstructor(new LoaderOptions()));
        try (InputStream is = plugin.resourceStream("translations/en.yml")) {
            this.translationFallback.putAll(yaml.load(is));
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to load default translation file", e);
        }
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.langParser, this.translationParser};
    }

    @Override
    public void delayedLoad() {
        this.clientLangData.values().forEach(LangData::processTranslations);
    }

    @Override
    public void reload() {
        // clear old data
        this.clientLangData.clear();
        this.installed.clear();
        this.translationKeys.clear();

        // save resources
        for (String lang : this.supportedLanguages) {
            this.plugin.saveResource("translations/" + lang + ".yml");
        }

        this.registry = MiniMessageTranslationRegistry.create(Key.key(net.momirealms.craftengine.core.util.Key.DEFAULT_NAMESPACE, "main"), AdventureHelper.miniMessage());
        this.registry.defaultLocale(DEFAULT_LOCALE);

        this.loadFromFileSystem(this.translationsDirectory);
        this.loadFromCache();
        MiniMessageTranslator.translator().setSource(this.registry);
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

    @Override
    public Set<String> translationKeys() {
        return translationKeys;
    }

    private void loadFromCache() {
        // 第一阶段：先注册所有没有国家/地区的locale
        for (Map.Entry<Locale, CachedTranslation> entry : this.cachedTranslations.entrySet()) {
            Locale locale = entry.getKey();
            // 只处理没有国家/地区的locale
            if (locale.getCountry().isEmpty()) {
                Map<String, String> translations = entry.getValue().translations();
                this.registry.registerAll(locale, translations);
                this.installed.add(locale);
            }
        }

        // 第二阶段：再注册其他完整的locale（包含国家/地区）
        for (Map.Entry<Locale, CachedTranslation> entry : this.cachedTranslations.entrySet()) {
            Locale locale = entry.getKey();
            // 跳过已经注册的无国家locale
            if (locale.getCountry().isEmpty()) {
                continue;
            }

            Map<String, String> translations = entry.getValue().translations();
            this.registry.registerAll(locale, translations);
            this.installed.add(locale);

            // 如果需要，为有国家/地区的locale也注册无国家版本
            Locale localeWithoutCountry = Locale.of(locale.getLanguage());
            if (!this.installed.contains(localeWithoutCountry) &&
                    !localeWithoutCountry.equals(DEFAULT_LOCALE)) {
                try {
                    this.registry.registerAll(localeWithoutCountry, translations);
                    this.installed.add(localeWithoutCountry);
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }
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
                            TranslationManagerImpl.this.plugin.logger().warn("Unknown locale '" + localeName + "' - unable to register.");
                            return FileVisitResult.CONTINUE;
                        }
                        CachedTranslation cachedFile = previousTranslations.get(locale);
                        long lastModifiedTime = attrs.lastModifiedTime().toMillis();
                        long size = attrs.size();
                        if (cachedFile != null && cachedFile.lastModified() == lastModifiedTime && cachedFile.size() == size) {
                            TranslationManagerImpl.this.cachedTranslations.put(locale, cachedFile);
                        } else {
                            try (InputStreamReader inputStream = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                                Yaml yaml = new Yaml(new TranslationConfigConstructor(new LoaderOptions()));
                                Map<String, String> data = yaml.load(inputStream);
                                if (data == null) return FileVisitResult.CONTINUE;
                                String langVersion = data.getOrDefault("lang-version", "");
                                if (!langVersion.equals(TranslationManagerImpl.this.langVersion) && TranslationManagerImpl.this.supportedLanguages.contains(localeName)) {
                                    data = updateLangFile(data, path);
                                }
                                cachedFile = new CachedTranslation(data, lastModifiedTime, size);
                                TranslationManagerImpl.this.cachedTranslations.put(locale, cachedFile);
                            } catch (IOException e) {
                                TranslationManagerImpl.this.plugin.logger().severe("Error while reading translation file: " + path, e);
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

    @Override
    public void log(String id, String... args) {
        String translation = miniMessageTranslation(id);
        if (translation == null || translation.isEmpty()) translation = id;
        this.plugin.senderFactory().console().sendMessage(AdventureHelper.miniMessage().deserialize(translation, new IndexedArgumentTag(Arrays.stream(args).map(Component::text).toList())));
    }

    private Map<String, String> updateLangFile(Map<String, String> previous, Path translationFile) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setSplitLines(false);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        Yaml yaml = new Yaml(new StringKeyConstructor(translationFile, new LoaderOptions()), new Representer(options), options);
        LinkedHashMap<String, String> newFileContents = new LinkedHashMap<>();
        try (InputStream is = this.plugin.resourceStream("translations/" + translationFile.getFileName())) {
            Map<String, String> newMap = yaml.load(is);
            newFileContents.putAll(this.translationFallback);
            newFileContents.putAll(newMap);
            // 思考是否值得特殊处理list类型的dump？似乎并没有这个必要。用户很少会使用list类型，且dump后只改变YAML结构而不影响游戏内效果。
            newFileContents.putAll(previous);
            newFileContents.put("lang-version", this.langVersion);
            String yamlString = yaml.dump(newFileContents);
            Files.writeString(translationFile, yamlString);
            return newFileContents;
        }
    }

    @Override
    public Map<String, LangData> clientLangData() {
        return Collections.unmodifiableMap(this.clientLangData);
    }

    @Override
    public void addClientTranslation(String langId, Map<String, String> translations) {
        if ("all".equals(langId)) {
            ALL_LANG.forEach(lang -> this.clientLangData.computeIfAbsent(lang, k -> new LangData())
                    .addTranslations(translations));
            return;
        }

        if (ALL_LANG.contains(langId)) {
            this.clientLangData.computeIfAbsent(langId, k -> new LangData())
                    .addTranslations(translations);
            return;
        }

        List<String> langCountries = LOCALE_2_COUNTRIES.getOrDefault(langId, Collections.emptyList());
        for (String lang : langCountries) {
            this.clientLangData.computeIfAbsent(langId + "_" + lang, k -> new LangData())
                    .addTranslations(translations);
        }
    }

    // 为了解决如下的格式兼容 a.b.c
    // a:
    //  b:
    //   c: xxx
    private static void loadLangKeyDeeply(String prefix, Map<String, Object> data, BiConsumer<String, String> collector) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map<?,?> map) {
                loadLangKeyDeeply(assembleLangKey(prefix, entry.getKey()), MiscUtils.castToMap(map, false), collector);
            } else {
                collector.accept(assembleLangKey(prefix, entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    private static String assembleLangKey(String prefix, String lang) {
        if (prefix.isEmpty()) {
            return lang;
        }
        return prefix + "." + lang;
    }

    private final class TranslationParser extends SectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"translations", "translation", "l10n", "localization", "i18n", "internationalization"};
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
            return LoadingStages.TRANSLATION;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        protected void parseSection(Pack pack, Path path, ConfigSection section) {
            Map<String, Object> locales = section.values();
            ExceptionCollector<ResourceException> collector = new ExceptionCollector<>();
            for (Map.Entry<String, Object> entry : locales.entrySet()) {
                String langId = entry.getKey();
                Locale locale = TranslationManager.parseLocale(langId);
                if (locale == null) {
                    collector.add(new KnownResourceException("resource.lang.unknown_locale", section.assemblePath(langId), langId));
                    continue;
                }
                Object langData = entry.getValue();
                if (!(langData instanceof Map<?,?> map)) {
                    collector.add(new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, section.assemblePath(langId), langData.getClass().getSimpleName()));
                    continue;
                }
                Map<String, String> bundle = new HashMap<>();
                loadLangKeyDeeply("", MiscUtils.castToMap(map, false), (key, value) -> {
                    bundle.put(key, value);
                    TranslationManagerImpl.this.translationKeys.add(key);
                });
                this.count += bundle.size();
                TranslationManagerImpl.this.registry.registerAll(locale, bundle);
            }
            collector.throwIfPresent();
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
            Map<String, Object> locales = section.values();
            ExceptionCollector<ResourceException> collector = new ExceptionCollector<>();
            for (Map.Entry<String, Object> entry : locales.entrySet()) {
                String langId = entry.getKey();
                Object langData = entry.getValue();
                if (!(langData instanceof Map<?,?> map)) {
                    collector.add(new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, section.assemblePath(langId), langData.getClass().getSimpleName()));
                    continue;
                }
                Map<String, String> sectionData = new HashMap<>();
                loadLangKeyDeeply("", MiscUtils.castToMap(map, false), (key, value) -> sectionData.put(key, LANG_FORMATTER.apply(value)));
                this.count += sectionData.size();
                TranslationManagerImpl.this.addClientTranslation(langId, sectionData);
            }
            collector.throwIfPresent();
        }
    }

    private record CachedTranslation(Map<String, String> translations, long lastModified, long size) {
    }
}
