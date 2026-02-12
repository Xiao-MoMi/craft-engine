package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;

import java.util.Map;

/**
 * 贝塔分布提供器
 * 极其灵活的分布，通过 alpha 和 beta 参数控制区间 [min, max] 内的形状
 */
public record BetaNumberProvider(
    double min,
    double max,
    double alpha, // 形状参数 α
    double beta   // 形状参数 β
) implements NumberProvider {
    public static final NumberProviderFactory<BetaNumberProvider> FACTORY = new Factory();

    public BetaNumberProvider {
        if (min >= max) throw new IllegalArgumentException("min < max required");
        if (alpha <= 0 || beta <= 0) throw new IllegalArgumentException("alpha, beta > 0 required");
    }

    @Override
    public float getFloat(RandomSource random) {
        return (float) getDouble(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        // 使用针对不同参数范围优化的生成算法
        double x = generateStandardBeta(this.alpha, this.beta, random);
        // 将 [0, 1] 映射到 [min, max]
        return this.min + x * (this.max - this.min);
    }

    /**
     * 生成标准 Beta(α, β) 分布 (范围 [0, 1])
     * 采用受阻采样法 (Rejection Sampling)
     */
    private double generateStandardBeta(double a, double b, RandomSource randomSource) {
        // 特例优化：如果 α=1, β=1，退化为均匀分布
        if (Math.abs(a - 1.0) < 1e-6 && Math.abs(b - 1.0) < 1e-6) {
            return randomSource.nextDouble();
        }

        // 简化的受阻采样实现 (针对 a, b >= 1 的常见场景)
        // 生产环境下如果 a, b < 1，通常建议使用 Gamma 分布转换法
        while (true) {
            double u1 = randomSource.nextDouble();
            double u2 = randomSource.nextDouble();
            double x = Math.pow(u1, 1.0 / a);
            double y = Math.pow(u2, 1.0 / b);
            
            if (x + y <= 1.0) {
                return x / (x + y);
            }
        }
    }

    @Override
    public int getInt(Context context) {
        return (int) Math.round(getDouble(context));
    }

    private static class Factory implements NumberProviderFactory<BetaNumberProvider> {
        @Override
        public BetaNumberProvider create(Map<String, Object> arguments) {
            double min = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("min", 0.0), "min");
            double max = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("max", 1.0), "max");
            
            // α 和 β 的默认值通常设为 2.0 (形成一个平滑的中间高两头低的弧线)
            double alpha = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("alpha", 2.0), "alpha");
            double beta = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("beta", 2.0), "beta");
            
            return new BetaNumberProvider(min, max, alpha, beta);
        }
    }
}