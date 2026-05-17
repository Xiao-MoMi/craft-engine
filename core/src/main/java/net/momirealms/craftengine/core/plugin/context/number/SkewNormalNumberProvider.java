package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.random.RandomSource;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 通用偏态分布提供器
 * <p>
 * 基于偏态正态分布（Skew Normal Distribution）实现。
 * 通过 Azallini 的方法生成：Z = δ|X| + sqrt(1-δ^2)Y
 */
public final class SkewNormalNumberProvider implements NumberProvider {
    public static final NumberProviderFactory<SkewNormalNumberProvider> FACTORY = new Factory();

    // 理论最大偏度 (approx 0.99527)
    private static final double MAX_SKEWNESS = 0.995;

    private final double min;
    private final double max;
    private final double targetMean;
    private final double targetStdDev;
    private final double skewness;
    private final int maxAttempts;

    // 预计算的分布参数
    private final double delta;          // 相关系数 δ
    private final double sqrtOneMinusDeltaSq; // √(1 - δ²) 用于生成公式优化
    private final double omega;          // 尺度参数 ω (Scale)
    private final double xi;             // 位置参数 ξ (Location)

    public SkewNormalNumberProvider(double min, double max, double mean, double stdDev, double skewness, int maxAttempts) {
        this.min = min;
        this.max = max;
        this.targetMean = mean;
        this.targetStdDev = stdDev;
        this.skewness = skewness;
        this.maxAttempts = maxAttempts;

        // 根据偏度计算形状相关参数 δ
        this.delta = calculateDelta(this.skewness);

        // 预计算生成公式中需要的常数，避免热点代码重复计算
        this.sqrtOneMinusDeltaSq = Math.sqrt(1 - this.delta * this.delta);

        // 计算尺度参数 ω
        // Var(X) = ω² * (1 - 2δ²/π)  =>  ω = stdDev / sqrt(1 - 2δ²/π)
        this.omega = stdDev / Math.sqrt(1 - (2 * this.delta * this.delta) / Math.PI);

        // 计算位置参数 ξ
        // E[X] = ξ + ω * δ * sqrt(2/π)  =>  ξ = mean - ω * δ * sqrt(2/π)
        this.xi = mean - this.omega * this.delta * Math.sqrt(2.0 / Math.PI);
    }

    /**
     * 根据目标偏度反推相关系数 δ
     * 公式推导基于: |γ1| = (4-π)/2 * (δ*sqrt(2/π))^3 / (1 - 2δ²/π)^(3/2)
     */
    private double calculateDelta(double skewness) {
        if (Math.abs(skewness) < 1e-6) {
            return 0.0;
        }

        double absGamma = Math.abs(skewness);
        // 为了数值稳定性，再次钳制范围
        absGamma = Math.min(absGamma, MAX_SKEWNESS);

        double sign = skewness < 0 ? -1.0 : 1.0;

        // 使用精确反函数解
        double term1 = Math.pow(absGamma, 2.0 / 3.0);
        double term2 = Math.pow((4.0 - Math.PI) / 2.0, 2.0 / 3.0);
        double deltaAbs = Math.sqrt((Math.PI / 2.0) * term1 / (term1 + term2));

        return sign * deltaAbs;
    }

    @Override
    public int getInt(RandomSource random) {
        // 四舍五入取整
        return (int) Math.round(getDouble(random));
    }

    @Override
    public float getFloat(RandomSource random) {
        return (float) getDouble(random);
    }

    @Override
    public double getDouble(RandomSource random) {
        // 如果没有偏度，直接使用更快的标准高斯生成
        if (Math.abs(this.skewness) < 1e-6) {
            return generateNormalBounded();
        }
        return generateSkewNormalBounded(random);
    }

    /**
     * 生成有界偏态分布随机数
     */
    private double generateSkewNormalBounded(RandomSource random) {
        for (int i = 0; i < this.maxAttempts; i++) {
            // 生成标准正态变量
            double u0 = random.nextGaussian();
            double u1 = random.nextGaussian();

            // 核心生成公式: Z = δ|U0| + √(1-δ²)U1
            // 此时 Z 服从标准偏态正态分布 (Location=0, Scale=1, Shape=α)
            double standardSkewed = this.delta * Math.abs(u0) + this.sqrtOneMinusDeltaSq * u1;

            // 转换到目标均值和方差: X = ξ + ωZ
            double value = this.xi + this.omega * standardSkewed;

            if (value >= this.min && value <= this.max) {
                return value;
            }
        }
        // 失败回退：返回区间内受限的均值
        return MiscUtils.clamp(this.targetMean, this.min, this.max);
    }

    /**
     * 特例优化：当偏度为0时（正态分布），使用更简单的逻辑
     */
    private double generateNormalBounded() {
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < this.maxAttempts; i++) {
            double value = this.targetMean + random.nextGaussian() * this.targetStdDev;
            if (value >= this.min && value <= this.max) {
                return value;
            }
        }
        return MiscUtils.clamp(this.targetMean, this.min, this.max);
    }

    private static class Factory implements NumberProviderFactory<SkewNormalNumberProvider> {
        private static final String[] STD_DEV = new String[] {"std_dev", "std-dev"};
        private static final String[] MAX_ATTEMPTS = new String[] {"max_attempts", "max-attempts"};

        @Override
        public SkewNormalNumberProvider create(ConfigSection section) {
            double min = section.getNonNullDouble("min");
            double max = section.getNonNullDouble("max");

            double defaultMean = (min + max) / 2.0;
            double mean = section.getDouble("mean", defaultMean);

            double defaultStdDev = (max - min) / 6.0;
            double stdDev = section.getDouble(STD_DEV, defaultStdDev);
            double skewness = section.getDouble("skewness");
            int maxAttempts = section.getInt(MAX_ATTEMPTS, 64);
            this.validateParameters(section.path(), min, max, stdDev, maxAttempts, skewness);
            return new SkewNormalNumberProvider(min, max, mean, stdDev, skewness, maxAttempts);
        }

        private void validateParameters(String path, double min, double max, double targetStdDev, int maxAttempts, double skewness) {
            if (min >= max) {
                throw new KnownResourceException("number.less_than", path, "min", "max");
            }
            if (targetStdDev <= 0) {
                throw new KnownResourceException("number.greater_than", path, "std_dev", "0");
            }
            if (maxAttempts <= 0) {
                throw new KnownResourceException("number.greater_than", path, "max_attempts", "0");
            }
            // 严格限制偏度，防止数学计算错误
            if (Math.abs(skewness) > MAX_SKEWNESS) {
                throw new IllegalArgumentException("skewness absolute value must be <= " + MAX_SKEWNESS);
            }
        }
    }
}