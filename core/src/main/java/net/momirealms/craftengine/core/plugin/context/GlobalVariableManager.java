package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.IdValueConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlobalVariableManager implements Manageable {
    public static final GlobalVariableManager INSTANCE = new GlobalVariableManager();
    private final Map<String, String> globalVariables = new HashMap<>(256, 0.5f);
    private final GlobalVariableParser parser = new GlobalVariableParser();

    private GlobalVariableManager() {
    }

    public static GlobalVariableManager instance() {
        return INSTANCE;
    }

    @Nullable
    public String get(final String key) {
        return this.globalVariables.get(key);
    }

    @Override
    public void unload() {
        this.globalVariables.clear();
    }

    public Map<String, String> globalVariables() {
        return Collections.unmodifiableMap(this.globalVariables);
    }

    public ConfigParser parser() {
        return this.parser;
    }

    private final class GlobalVariableParser extends IdValueConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {
                "global-variables", "global-variable",
                "global_variables", "global_variable"
        };

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return GlobalVariableManager.this.globalVariables.size();
        }

        @Override
        public void parseValue(Pack pack, Path filePath, Key id, ConfigValue value) {
            GlobalVariableManager.this.globalVariables.put(id.value(), value.getAsString());
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.GLOBAL_VARIABLE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }
    }
}
