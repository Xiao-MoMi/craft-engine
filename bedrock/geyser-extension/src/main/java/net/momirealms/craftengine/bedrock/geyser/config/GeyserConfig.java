package net.momirealms.craftengine.bedrock.geyser.config;

import net.momirealms.craftengine.bedrock.geyser.GeyserProperties;
import net.momirealms.sparrow.yaml.SparrowYaml;
import net.momirealms.sparrow.yaml.YamlDocument;
import net.momirealms.sparrow.yaml.route.Route;
import net.momirealms.sparrow.yaml.upgrade.YamlUpgradeBuilder;
import net.momirealms.sparrow.yaml.upgrade.YamlUpgradePipeline;
import org.geysermc.geyser.api.extension.ExtensionLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class GeyserConfig {
    private static GeyserConfig instance;
    private final Path configFilePath;
    private final ExtensionLogger logger;
    private final SparrowYaml yaml = SparrowYaml.builder().build();
    private final String configVersion = GeyserProperties.get("config");
    private YamlDocument config;
    private long lastModified;
    private long size;
    private String forcedLocale = "";

    public GeyserConfig(Path directory, ExtensionLogger logger) {
        this.configFilePath = directory.resolve("config.yml");
        this.logger = logger;
        instance = this;
    }

    public void load() {
        if (this.updateConfigCache()) {
            this.loadFullSettings();
        }
    }

    public boolean updateConfigCache() {
        try {
            if (Files.notExists(this.configFilePath)) {
                Files.createDirectories(this.configFilePath.getParent());
                try (InputStream stream = GeyserConfig.class.getResourceAsStream("/config.yml")) {
                    if (stream == null) {
                        throw new IOException("Embedded config.yml not found");
                    }
                    Files.copy(stream, this.configFilePath);
                }
            }
            BasicFileAttributes attributes = Files.readAttributes(this.configFilePath, BasicFileAttributes.class);
            long lastModified = attributes.lastModifiedTime().toMillis();
            long size = attributes.size();
            if (this.config != null && this.lastModified == lastModified && this.size == size) {
                return false;
            }
            YamlDocument loadedConfig = this.yaml.load(this.configFilePath);
            if (!this.configVersion.equals(ConfigVersionExtractor.INSTANCE.extractVersion(loadedConfig))) {
                loadedConfig = this.updateConfigVersion(loadedConfig);
            }
            this.config = loadedConfig;
            this.updateConfigFileAttributes();
            return true;
        } catch (IOException | RuntimeException exception) {
            this.logger.error("Failed to update config.yml", exception);
            return false;
        }
    }

    private YamlDocument updateConfigVersion(YamlDocument loadedConfig) throws IOException {
        YamlDocument upgradedConfig;
        try (InputStream stream = GeyserConfig.class.getResourceAsStream("/config.yml")) {
            if (stream == null) {
                throw new IOException("Embedded config.yml not found");
            }
            YamlUpgradePipeline pipeline = yamlUpgradeBuilder()
                    .updateComments(true)
                    .deleteRemovedNodes(true)
                    .build();
            upgradedConfig = pipeline.upgrade(loadedConfig, this.yaml.load(stream));
        }
        upgradedConfig.save(this.configFilePath);
        return upgradedConfig;
    }

    public static YamlUpgradeBuilder yamlUpgradeBuilder() {
        return YamlUpgradePipeline.builder().versionExtractor(ConfigVersionExtractor.INSTANCE);
    }

    private void updateConfigFileAttributes() throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(this.configFilePath, BasicFileAttributes.class);
        this.lastModified = attributes.lastModifiedTime().toMillis();
        this.size = attributes.size();
    }

    private void loadFullSettings() {
        this.forcedLocale = this.config.getStringOrDefault("", Route.from("forced-locale"));
    }

    public static String forcedLocale() {
        return instance.forcedLocale;
    }
}
