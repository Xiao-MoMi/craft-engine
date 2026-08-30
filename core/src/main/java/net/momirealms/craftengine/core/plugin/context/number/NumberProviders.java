package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.expression.ContextExpression;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public final class NumberProviders {
    public static final NumberProviderType<ConstantNumberProvider> FIXED = register(Key.ce("fixed"), ConstantNumberProvider.FACTORY);
    public static final NumberProviderType<ConstantNumberProvider> CONSTANT = register(Key.ce("constant"), ConstantNumberProvider.FACTORY);
    public static final NumberProviderType<UniformNumberProvider> UNIFORM = register(Key.ce("uniform"), UniformNumberProvider.FACTORY);
    public static final NumberProviderType<ExpressionNumberProvider> EXPRESSION = register(Key.ce("expression"), ExpressionNumberProvider.FACTORY);
    public static final NumberProviderType<GaussianNumberProvider> NORMAL = register(Key.ce("normal"), GaussianNumberProvider.FACTORY);
    public static final NumberProviderType<GaussianNumberProvider> GAUSSIAN = register(Key.ce("gaussian"), GaussianNumberProvider.FACTORY);
    public static final NumberProviderType<LogNormalNumberProvider> LOG_NORMAL = register(Key.ce("log_normal"), LogNormalNumberProvider.FACTORY);
    public static final NumberProviderType<SkewNormalNumberProvider> SKEW_NORMAL = register(Key.ce("skew_normal"), SkewNormalNumberProvider.FACTORY);
    public static final NumberProviderType<BinomialNumberProvider> BINOMIAL = register(Key.ce("binomial"), BinomialNumberProvider.FACTORY);
    public static final NumberProviderType<WeightedNumberProvider> WEIGHTED = register(Key.ce("weighted"), WeightedNumberProvider.FACTORY);
    public static final NumberProviderType<TriangleNumberProvider> TRIANGLE = register(Key.ce("triangle"), TriangleNumberProvider.FACTORY);
    public static final NumberProviderType<ExponentialNumberProvider> EXPONENTIAL = register(Key.ce("exponential"), ExponentialNumberProvider.FACTORY);
    public static final NumberProviderType<BetaNumberProvider> BETA = register(Key.ce("beta"), BetaNumberProvider.FACTORY);

    private NumberProviders() {}

    public static <T extends NumberProvider> NumberProviderType<T> register(Key key, NumberProviderFactory<T> factory) {
        NumberProviderType<T> type = new NumberProviderType<>(key, factory);
        ((WritableRegistry<NumberProviderType<? extends NumberProvider>>) BuiltInRegistries.NUMBER_PROVIDER_TYPE)
                .register(ResourceKey.create(Registries.NUMBER_PROVIDER_TYPE.location(), key), type);
        return type;
    }

    public static NumberProvider direct(double value) {
        return new ConstantNumberProvider(value);
    }

    public static NumberProvider fromConfig(ConfigSection section) {
        String type = section.getNonNullString("type");
        Key key = Key.ce(type);
        NumberProviderType<? extends NumberProvider> providerType = BuiltInRegistries.NUMBER_PROVIDER_TYPE.getValue(key);
        if (providerType == null) {
            throw new KnownResourceException("number.unknown_type", section.assemblePath("type"), type);
        }
        return providerType.factory().create(section);
    }

    public static NumberProvider fromConfig(ConfigValue value) {
        return switch (value.value()) {
            case Number number -> ConstantNumberProvider.constant(number.doubleValue());
            case Boolean bool -> ConstantNumberProvider.constant(bool ? 1 : 0);
            case Map<?, ?> ignored -> NumberProviders.fromConfig(value.getAsSection());
            default -> fromString(value);
        };
    }

    private static NumberProvider fromString(ConfigValue value) {
        String source = value.getAsString().trim();
        int separator = findRangeSeparator(source);
        if (separator >= 0) {
            return new UniformNumberProvider(
                    parseScalar(value.path(), source.substring(0, separator)),
                    parseScalar(value.path(), source.substring(separator + 1))
            );
        }
        return parseScalar(value.path(), source);
    }

    private static NumberProvider parseScalar(String path, String source) {
        source = source.trim();
        Double literal = tryParseLiteral(source);
        if (literal != null) {
            return ConstantNumberProvider.constant(literal);
        }

        ContextExpression<Context> expression = ContextExpression.precompile(path, source);
        ExpressionNumberProvider provider = new ExpressionNumberProvider(expression);
        return provider.isConstant()
                ? ConstantNumberProvider.constant(provider.getDouble())
                : provider;
    }

    private static int findRangeSeparator(String source) {
        int separator = -1;
        int parentheses = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '(') {
                parentheses++;
            } else if (current == ')' && parentheses > 0) {
                parentheses--;
            } else if (current == '~' && parentheses == 0) {
                if (separator >= 0) {
                    return -1;
                }
                separator = i;
            }
        }
        return separator;
    }

    private static Double tryParseLiteral(String source) {
        try {
            return Double.parseDouble(source.trim().replace("_", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
