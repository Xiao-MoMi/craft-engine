package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathMatchers {
    public static final Key EXACT = Key.of("craftengine:exact");
    public static final Key CONTAINS = Key.of("craftengine:contains");
    public static final Key FILENAME = Key.of("craftengine:filename");
    public static final Key PARENT_PATH_SUFFIX = Key.of("craftengine:parent_path_suffix");
    public static final Key PARENT_PATH_PREFIX = Key.of("craftengine:parent_path_prefix");
    public static final Key PATTERN = Key.of("craftengine:pattern");

    static {
        register(CommonConditions.ANY_OF, new AnyOfCondition.FactoryImpl<>(PathMatchers::fromMap));
        register(CommonConditions.ALL_OF, new AllOfCondition.FactoryImpl<>(PathMatchers::fromMap));
        register(CommonConditions.INVERTED, new InvertedCondition.FactoryImpl<>(PathMatchers::fromMap));
        register(PARENT_PATH_SUFFIX, new PathMatcherParentSuffix.FactoryImpl());
        register(PARENT_PATH_PREFIX, new PathMatcherParentPrefix.FactoryImpl());
        register(PATTERN, new PathPatternMatcher.FactoryImpl());
        register(EXACT, new PathMatcherExact.FactoryImpl());
        register(FILENAME, new PathMatcherFilename.FactoryImpl());
        register(CONTAINS, new PathMatcherContains.FactoryImpl());
    }

    public static void register(Key key, ConditionFactory<PathContext> factory) {
        ((WritableRegistry<ConditionFactory<PathContext>>) BuiltInRegistries.PATH_MATCHER_FACTORY)
                .register(ResourceKey.create(Registries.PATH_MATCHER_FACTORY.location(), key), factory);
    }

    public static List<Condition<PathContext>> fromMapList(List<Map<String, Object>> arguments) {
        List<Condition<PathContext>> matchers = new ArrayList<>();
        for (Map<String, Object> term : arguments) {
            matchers.add(PathMatchers.fromMap(term));
        }
        return matchers;
    }

    public static Condition<PathContext> fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), () -> new LocalizedException("warning.config.conflict_matcher.missing_type"));
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        if (key.value().charAt(0) == '!') {
            ConditionFactory<PathContext> factory = BuiltInRegistries.PATH_MATCHER_FACTORY.getValue(new Key(key.namespace(), key.value().substring(1)));
            if (factory == null) {
                throw new LocalizedException("warning.config.conflict_matcher.invalid_type", type);
            }
            return new InvertedCondition<>(factory.create(map));
        } else {
            ConditionFactory<PathContext> factory = BuiltInRegistries.PATH_MATCHER_FACTORY.getValue(key);
            if (factory == null) {
                throw new LocalizedException("warning.config.conflict_matcher.invalid_type", type);
            }
            return factory.create(map);
        }
    }
}
