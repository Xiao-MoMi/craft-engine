package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.AllOfCondition;
import net.momirealms.craftengine.core.plugin.context.condition.AnyOfCondition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.plugin.context.condition.InvertedCondition;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class PathMatchers {
    public static final PathMatcherType<AnyOfCondition<PathContext>> ANY_OF = register(Key.ce("any_of"), AnyOfCondition.factory(PathMatchers::fromConfig));
    public static final PathMatcherType<AllOfCondition<PathContext>> ALL_OF = register(Key.ce("all_of"), AllOfCondition.factory(PathMatchers::fromConfig));
    public static final PathMatcherType<InvertedCondition<PathContext>> INVERTED = register(Key.ce("inverted"), InvertedCondition.factory(PathMatchers::fromConfig));
    public static final PathMatcherType<ContainsPathMatcher> CONTAINS = register(Key.ce("contains"), ContainsPathMatcher.FACTORY);
    public static final PathMatcherType<ExactPathMatcher> EXACT = register(Key.ce("exact"), ExactPathMatcher.FACTORY);
    public static final PathMatcherType<FilenamePathMatcher> FILENAME = register(Key.ce("filename"), FilenamePathMatcher.FACTORY);
    public static final PathMatcherType<PatternPathMatcher> PATTERN = register(Key.ce("pattern"), PatternPathMatcher.FACTORY);
    public static final PathMatcherType<ParentSuffixPathMatcher> PARENT_PATH_SUFFIX = register(Key.ce("parent_path_suffix"), ParentSuffixPathMatcher.FACTORY);
    public static final PathMatcherType<ParentPrefixPathMatcher> PARENT_PATH_PREFIX = register(Key.ce("parent_path_prefix"), ParentPrefixPathMatcher.FACTORY);

    private PathMatchers() {}

    public static <T extends Condition<PathContext>> PathMatcherType<T> register(Key key, ConditionFactory<PathContext, T> factory) {
        PathMatcherType<T> type = new PathMatcherType<>(key, factory);
        ((WritableRegistry<PathMatcherType<?>>) BuiltInRegistries.PATH_MATCHER_TYPE)
                .register(ResourceKey.create(Registries.PATH_MATCHER_TYPE.location(), key), type);
        return type;
    }

    public static Condition<PathContext> fromConfig(ConfigSection section) {
        String type = section.getNonEmptyString("type");
        boolean reverted = type.charAt(0) == '!';
        if (reverted) {
            type = type.substring(1);
        }
        Key key = Key.ce(type);
        PathMatcherType<? extends Condition<PathContext>> matcherType = BuiltInRegistries.PATH_MATCHER_TYPE.getValue(key);
        if (matcherType == null) {
            throw new KnownResourceException("condition.unknown_type", section.assemblePath("type"), key.asString());
        }
        return reverted ? InvertedCondition.inverted(matcherType.factory().create(section)) : matcherType.factory().create(section);
    }
}
