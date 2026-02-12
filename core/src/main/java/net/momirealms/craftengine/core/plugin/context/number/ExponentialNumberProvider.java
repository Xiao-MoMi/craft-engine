package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 指数分布提供器
 * 用于描述独立随机事件发生的时间间隔
 * 参数 lambda (λ) 是单位时间内事件发生的平均次数 (率参数)
 */
public record ExponentialNumberProvider(
    double min,
    double max,
    double lambda,
    int maxAttempts
) implements NumberProvider {
    public static final NumberProviderFactory<ExponentialNumberProvider> FACTORY = new Factory();

    public ExponentialNumberProvider {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        if (lambda <= 0) {
            throw new IllegalArgumentException("lambda must be greater than 0");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("max-attempts must be greater than 0");
        }
    }

    @Override
    public int getInt(RandomSource random) {
        return (int) Math.round(getDouble(random));
    }

    @Override
    public float getFloat(RandomSource random) {
        return (float) getDouble(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        for (int i = 0; i < this.maxAttempts; i++) {
            // 逆变换采样法 (Inverse Transform Sampling)
            // 公式: X = -ln(1 - U) / λ  或者简单的 -ln(U) / λ
            // 其中 U 是 [0, 1) 之间的均匀分布随机数
            double u = random.nextDouble();
            
            // 防止 u 为 0 导致 ln(0) 出现负无穷
            if (u < 1e-10) continue;
            
            double value = -Math.log(u) / this.lambda;

            if (value >= this.min && value <= this.max) {
                return value;
            }
        }

        // 失败回退：返回 1/lambda (分布的期望均值)
        return MiscUtils.clamp(1.0 / this.lambda, this.min, this.max);
    }

    private static class Factory implements NumberProviderFactory<ExponentialNumberProvider> {
        @Override
        public ExponentialNumberProvider create(Map<String, Object> arguments) {
            double min = ResourceConfigUtils.getAsDouble(
                arguments.getOrDefault("min", 0.0), "min");
            
            double max = ResourceConfigUtils.getAsDouble(
                arguments.getOrDefault("max", Double.MAX_VALUE), "max");
            
            // 如果用户没填 lambda，尝试从 mean (均值) 转换
            // 指数分布中: mean = 1/lambda
            double lambda;
            if (arguments.containsKey("mean")) {
                double mean = ResourceConfigUtils.getAsDouble(arguments.get("mean"), "mean");
                lambda = 1.0 / mean;
            } else {
                lambda = ResourceConfigUtils.getAsDouble(
                    ResourceConfigUtils.requireNonNullOrThrow(arguments.get("lambda"), 
                    "warning.config.number.exponential.missing_lambda"), "lambda");
            }
            
            int maxAttempts = ResourceConfigUtils.getAsInt(
                arguments.getOrDefault("max-attempts", 64), "max-attempts");
            
            return new ExponentialNumberProvider(min, max, lambda, maxAttempts);
        }
    }

    @Override
    public @NotNull String toString() {
        return String.format("ExponentialNumberProvider{min=%.2f, max=%.2f, lambda=%.4f, mean=%.2f}",
                this.min, this.max, this.lambda, 1.0 / this.lambda);
    }
}