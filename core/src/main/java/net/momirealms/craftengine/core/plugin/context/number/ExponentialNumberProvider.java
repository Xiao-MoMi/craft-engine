package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;

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
        private static final String[] MAX_ATTEMPTS = new String[] {"max_attempts", "max-attempts"};

        @Override
        public ExponentialNumberProvider create(ConfigSection section) {
            double min = section.getDouble("min", 0d);
            double max = section.getDouble("max", Double.MAX_VALUE);
            
            // 如果用户没填 lambda，尝试从 mean (均值) 转换
            // 指数分布中: mean = 1/lambda
            double lambda;
            if (section.containsKey("mean")) {
                double mean = section.getNonNullDouble("mean");
                lambda = 1.0 / mean;
            } else {
                lambda = section.getNonNullDouble("lambda");
            }
            
            int maxAttempts = section.getInt(MAX_ATTEMPTS, 64);
            validateParameters(section.path(), min, max, lambda, maxAttempts);
            return new ExponentialNumberProvider(min, max, lambda, maxAttempts);
        }

        private void validateParameters(String path, double min, double max, double lambda, int maxAttempts) {
            if (min >= max) {
                throw new KnownResourceException("number.less_than", path, "min", "max");
            }
            if (lambda <= 0) {
                throw new KnownResourceException("number.greater_than", path, "lambda", "0");
            }
            if (maxAttempts <= 0) {
                throw new KnownResourceException("number.greater_than", path, "max_attempts", "0");
            }
        }
    }
}