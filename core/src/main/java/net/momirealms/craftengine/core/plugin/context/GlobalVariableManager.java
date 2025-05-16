package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GlobalVariableManager implements Manageable {
    private final Map<String, String> globalVariables = new HashMap<>();
    private final GlobalVariableParser parser = new GlobalVariableParser();

    @Nullable
    public String get(final String key) {
        return this.globalVariables.get(key);
    }

    @Override
    public void unload() {
        this.globalVariables.clear();
    }

    public ConfigParser parser() {
        return this.parser;
    }

    public class GlobalVariableParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"global-variables", "global-variable"};

        @Override
        public int loadingSequence() {
            return LoadingSequence.GLOBAL_VAR;
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public boolean supportsParsingObject() {
            return true;
        }

        @Override
        public void parseObject(Pack pack, Path path, net.momirealms.craftengine.core.util.Key id, Object object) throws LocalizedException {
            if (object != null) {
                globalVariables.put(id.value(), object.toString());
            }
        }
    }
}
