package net.momirealms.craftengine.core.plugin.locale;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TranslationManagerImpl implements TranslationManager {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private Locale forceLocale = null;
    protected static TranslationManager instance;

    private final Plugin plugin;
    private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
    private MiniMessageTranslationRegistry registry;
    private final Path translationsDirectory;
    private final Map<Locale, I18NData> i18nData = new HashMap<>();

    public TranslationManagerImpl(Plugin plugin) {
        this.plugin = plugin;
        this.translationsDirectory = this.plugin.dataFolderPath().resolve("translations");
        instance = this;
    }

    @Override
    public void forceLocale(Locale locale) {
        forceLocale = locale;
    }

    @Override
    public void reload() {
        // clear old data
        this.i18nData.clear();

        // remove any previous registry
        if (this.registry != null) {
            MiniMessageTranslator.translator().removeSource(this.registry);
            this.installed.clear();
        }

        String supportedLocales = PluginProperties.getValue("lang");
        for (String lang : supportedLocales.split(",")) {
            this.plugin.saveResource("translations/" + lang + ".yml");
        }

        this.registry = MiniMessageTranslationRegistry.create(Key.key("craftengine", "main"), AdventureHelper.miniMessage());
        this.registry.defaultLocale(DEFAULT_LOCALE);
        this.loadFromFileSystem(this.translationsDirectory, false);
        MiniMessageTranslator.translator().addSource(this.registry);
    }

    @Override
    public String miniMessageTranslation(String key, @Nullable Locale locale) {
        if (forceLocale != null) {
            return registry.miniMessageTranslation(key, forceLocale);
        }
        if (locale == null) {
            locale = Locale.getDefault();
            if (locale == null) {
                locale = DEFAULT_LOCALE;
            }
        }
        return registry.miniMessageTranslation(key, locale);
    }

    @Override
    public Component render(Component component, @Nullable Locale locale) {
        if (forceLocale != null) {
            return MiniMessageTranslator.render(component, forceLocale);
        }
        if (locale == null) {
            locale = Locale.getDefault();
            if (locale == null) {
                locale = DEFAULT_LOCALE;
            }
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

        Locale localLocale = Locale.getDefault();
        if (!this.installed.contains(localLocale) && this.forceLocale == null) {
            this.plugin.logger().warn(localLocale.toString().toLowerCase(Locale.ENGLISH) + ".yml not exists, using " + DEFAULT_LOCALE.toString().toLowerCase(Locale.ENGLISH) + ".yml as default locale.");
        }
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
        YamlDocument document = ConfigManager.instance().loadYamlConfig("translations" + File.separator + translationFile.getFileName(),
                GeneralSettings.DEFAULT,
                LoaderSettings
                        .builder()
                        .setAutoUpdate(true)
                        .build(),
                DumperSettings.DEFAULT,
                UpdaterSettings
                        .builder()
                        .setVersioning(new BasicVersioning("config-version"))
                        .build()
        );
        try {
            document.save(new File(this.plugin.dataFolderFile(), "translations" + File.separator + translationFile.getFileName()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not update translation file: " + translationFile.getFileName(), e);
        }
        Map<String, Object> map = document.getStringRouteMappedValues(false);
        map.remove("config-version");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                List<String> strList = (List<String>) list;
                StringJoiner stringJoiner = new StringJoiner("<reset><newline>");
                for (String str : strList) {
                    stringJoiner.add(str);
                }
                bundle.put(entry.getKey(), stringJoiner.toString());
            } else if (entry.getValue() instanceof String str) {
                bundle.put(entry.getKey(), str);
            }
        }

        this.registry.registerAll(locale, bundle);
        this.installed.add(locale);

        return Pair.of(locale, bundle);
    }

    @Override
    public String translateI18NTag(String i18nId) {
        Locale locale = forceLocale;
        if (locale == null) {
            locale = Locale.getDefault();
            if (locale == null) {
                I18NData data = i18nData.get(DEFAULT_LOCALE);
                if (data == null) return i18nId;
                return data.translate(i18nId);
            }
        }
        I18NData data = i18nData.get(locale);
        if (data == null) {
            Locale lang = Locale.of(locale.getLanguage());
            if (locale != lang) {
                data = i18nData.get(lang);
                if (data == null) {
                    return i18nId;
                }
            } else {
                return i18nId;
            }
        }
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

    public static class I18NData {
        private final Map<String, String> translations = new HashMap<>();

        public void addTranslation(String key, String value) {
            this.translations.put(key, value);
        }

        public String translate(String key) {
            return this.translations.getOrDefault(key, key);
        }
    }
}
