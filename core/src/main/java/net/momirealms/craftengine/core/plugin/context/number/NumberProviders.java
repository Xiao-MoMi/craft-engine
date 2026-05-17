package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
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
        switch (value.value()) {
            case Number number -> {
                return ConstantNumberProvider.constant(number.doubleValue());
            }
            case Boolean bool -> {
                return ConstantNumberProvider.constant(bool ? 1 : 0);
            }
            case Map<?, ?> ignored -> {
                return NumberProviders.fromConfig(value.getAsSection());
            }
            default -> {
                String string = value.getAsString();
                if (string.contains("~")) {
                    String[] split = string.split("~", 2);
                    double min;
                    try {
                        min = Double.parseDouble(split[0]);
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, value.path(), split[0]);
                    }
                    double max;
                    try {
                        max = Double.parseDouble(split[1]);
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, value.path(), split[1]);
                    }
                    return new UniformNumberProvider(ConstantNumberProvider.constant(min), ConstantNumberProvider.constant(max));
                } else if (string.contains("<") && string.contains(">")) {
                    return ExpressionNumberProvider.expression(string);
                } else {
                    try {
                        return ConstantNumberProvider.constant(Double.parseDouble(string));
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, value.path(), string);
                    }
                }
            }
        }
    }
}
