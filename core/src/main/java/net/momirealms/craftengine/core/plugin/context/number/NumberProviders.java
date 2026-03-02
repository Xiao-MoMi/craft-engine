package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
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

    @SuppressWarnings("unchecked")
    public static NumberProvider fromObject(Object object) {
        switch (object) {
            case null -> throw new LocalizedResourceConfigException("warning.config.number.missing_argument");
            case Number number -> {
                return new ConstantNumberProvider(number.floatValue());
            }
            case Boolean bool -> {
                return new ConstantNumberProvider(bool ? 1 : 0);
            }
            case Map<?, ?> map -> {
                return fromConfig(ConfigSection.ofRoot((Map<String, Object>) map)); // todo 更好的传递路径
            }
            default -> {
                String string = object.toString();
                if (string.contains("~")) {
                    int first = string.indexOf('~');
                    int second = string.indexOf('~', first + 1);
                    if (second == -1) {
                        NumberProvider min = fromObject(string.substring(0, first));
                        NumberProvider max = fromObject(string.substring(first + 1));
                        return new UniformNumberProvider(min, max);
                    } else {
                        throw new LocalizedResourceConfigException("warning.config.number.invalid_format", string);
                    }
                } else if (string.contains("<") && string.contains(">") && string.contains(":")) {
                    return new ExpressionNumberProvider(string);
                } else {
                    try {
                        return new ConstantNumberProvider(Float.parseFloat(string));
                    } catch (NumberFormatException e) {
                        throw new LocalizedResourceConfigException("warning.config.number.invalid_format", e, string);
                    }
                }
            }
        }
    }
}
