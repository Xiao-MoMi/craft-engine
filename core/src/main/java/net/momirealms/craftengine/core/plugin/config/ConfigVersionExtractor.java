package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.util.YamlUtils;
import net.momirealms.sparrow.yaml.YamlDocument;
import net.momirealms.sparrow.yaml.route.Route;
import net.momirealms.sparrow.yaml.upgrade.version.FieldVersionExtractor;
import net.momirealms.sparrow.yaml.upgrade.version.VersionExtractor;

final class ConfigVersionExtractor implements VersionExtractor {
    static final String VERSION_KEY = "___version___";
    static final Route VERSION_ROUTE = YamlUtils.route(VERSION_KEY);
    static final ConfigVersionExtractor INSTANCE = new ConfigVersionExtractor();
    private static final FieldVersionExtractor VERSION_EXTRACTOR = new FieldVersionExtractor(VERSION_ROUTE);

    private ConfigVersionExtractor() {
    }

    @Override
    public String extractVersion(YamlDocument document) {
        if (document.contains(VERSION_ROUTE)) {
            return VERSION_EXTRACTOR.extractVersion(document);
        }
        return "0";
    }

    @Override
    public String extractTargetVersion(YamlDocument document) {
        return VERSION_EXTRACTOR.extractVersion(document);
    }

    @Override
    public void writeVersion(YamlDocument document, String version) {
        VERSION_EXTRACTOR.writeVersion(document, version);
    }
}
