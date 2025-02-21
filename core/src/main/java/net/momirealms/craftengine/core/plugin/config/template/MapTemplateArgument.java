package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class MapTemplateArgument implements TemplateArgument {
    public static final Factory FACTORY = new Factory();
    private final Map<String, Object> value;

    public MapTemplateArgument(Map<String, Object> value) {
        this.value = value;
    }

    @Override
    public Map<String, Object> get() {
        return value;
    }

    @Override
    public Key type() {
        return TemplateArguments.MAP;
    }

    public static class Factory implements TemplateArgumentFactory {

        @Override
        public TemplateArgument create(Map<String, Object> arguments) {
            return new MapTemplateArgument(MiscUtils.castToMap(arguments.getOrDefault("map", Map.of()), false));
        }
    }
}
